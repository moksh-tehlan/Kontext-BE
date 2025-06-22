## 📨 Data Exchange & Queue Message Contracts

### **🔧 RabbitMQ Queue Configuration**

```yaml
# Exchange Configuration
exchanges:
  document.processing:
    type: topic
    durable: true
    auto_delete: false
    
  url.processing:
    type: topic  
    durable: true
    auto_delete: false

# Queue Configuration
queues:
  # Input Queues (Spring Boot → Python)
  document.processing.queue:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "document.process.*"
    arguments:
      x-message-ttl: 3600000  # 1 hour TTL
      x-max-retries: 3
      
  url.processing.queue:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "url.process.*"
    arguments:
      x-message-ttl: 3600000  # 1 hour TTL
      x-max-retries: 3
  
  # Output Queues (Python → Spring Boot)
  document.processing.success:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "document.success.*"
    
  document.processing.error:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "document.error.*"
    
  document.processing.status:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "document.status.*"
    
  url.processing.success:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "url.success.*"
    
  url.processing.error:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "url.error.*"
    
  url.processing.status:
    durable: true
    exclusive: false
    auto_delete: false
    routing_key: "url.status.*"

# Dead Letter Queues for Failed Messages
dead_letter_queues:
  document.processing.dlq:
    durable: true
    routing_key: "document.failed.*"
    
  url.processing.dlq:
    durable: true
    routing_key: "url.failed.*"
```

### **📦 S3 Storage Structure & Configuration**

```yaml
# S3 Bucket Structure
bucket: mobile-rag-documents
region: us-east-1

# Directory Structure
/users/{userId}/
  /projects/{projectId}/
    /documents/
      /{fileName}_{timestamp}.{ext}  # Original uploaded files
    /assets/
      /images/
        /{extractedImage}_{timestamp}.png
      /tables/
        /{extractedTable}_{timestamp}.json
    /exports/
      /{conversationExport}_{timestamp}.{format}

# S3 Configuration
storage_class: STANDARD
encryption: AES256
versioning: Enabled
lifecycle_policy:
  - id: transition_to_ia
    status: Enabled
    transition:
      days: 30
      storage_class: STANDARD_IA
  - id: transition_to_glacier
    status: Enabled
    transition:
      days: 90
      storage_class: GLACIER
  - id: delete_old_exports
    status: Enabled
    expiration:
      days: 365  # Delete conversation exports after 1 year

# Access Policies
bucket_policy:
  principals:
    - spring_boot_service_role
    - python_processing_service_role
  actions:
    - s3:GetObject
    - s3:PutObject
    - s3:DeleteObject
  resources:
    - "arn:aws:s3:::mobile-rag-documents/*"
```

### **🔄 Message Flow Implementation Examples**

#### **Spring Boot: Publishing Document Processing Message**
```java
@Service
public class DocumentProcessingService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private S3Service s3Service;
    
    public void processDocument(MultipartFile file, Long projectId, Long knowledgeSourceId) {
        try {
            // 1. Upload to S3
            String s3Key = s3Service.uploadFile(file, projectId);
            
            // 2. Create processing message
            DocumentProcessingMessage message = DocumentProcessingMessage.builder()
                .messageType("PROCESS_DOCUMENT")
                .jobId(UUID.randomUUID().toString())
                .userId(getCurrentUserId())
                .projectId(projectId)
                .knowledgeSourceId(knowledgeSourceId)
                .documentInfo(DocumentInfo.builder()
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .s3Location(S3Location.builder()
                        .bucket("mobile-rag-documents")
                        .key(s3Key)
                        .region("us-east-1")
                        .build())
                    .uploadedAt(Instant.now())
                    .build())
                .processingOptions(ProcessingOptions.builder()
                    .chunkSize(1000)
                    .chunkOverlap(200)
                    .extractImages(false)
                    .extractTables(true)
                    .language("en")
                    .build())
                .userPlan(getCurrentUserPlan())
                .callbackConfig(CallbackConfig.builder()
                    .successQueue("document.processing.success")
                    .errorQueue("document.processing.error")
                    .statusQueue("document.processing.status")
                    .build())
                .build();
            
            // 3. Publish to RabbitMQ
            rabbitTemplate.convertAndSend(
                "document.processing",
                "document.process.new",
                message
            );
            
            // 4. Update knowledge source status
            knowledgeSourceService.updateStatus(knowledgeSourceId, ProcessingStatus.PROCESSING);
            
        } catch (Exception e) {
            log.error("Failed to initiate document processing", e);
            knowledgeSourceService.updateStatus(knowledgeSourceId, ProcessingStatus.FAILED);
        }
    }
}
```

#### **Spring Boot: Consuming Processing Results**
```java
@Component
public class ProcessingResultConsumer {
    
    @Autowired
    private VectorStore knowledgeVectorStore;
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @RabbitListener(queues = "document.processing.success")
    public void handleDocumentProcessingSuccess(DocumentProcessingResult result) {
        try {
            log.info("Processing completed for job: {}", result.getJobId());
            
            // 1. Convert chunks to Document objects for vector storage
            List<Document> documents = result.getProcessingResult().getChunks().stream()
                .map(chunk -> new Document(
                    chunk.getContent(),
                    Map.of(
                        "project_id", result.getKnowledgeSourceId(),
                        "knowledge_source_id", result.getKnowledgeSourceId(),
                        "chunk_id", chunk.getChunkId(),
                        "chunk_index", chunk.getChunkIndex(),
                        "source_name", result.getProcessingResult().getExtractedContent().getMetadata().getTitle(),
                        "source_type", "DOCUMENT",
                        "processed_at", result.getProcessingResult().getProcessedAt().toString()
                    )
                ))
                .collect(Collectors.toList());
            
            // 2. Generate embeddings and store in Qdrant
            knowledgeVectorStore.add(documents);
            
            // 3. Update knowledge source status
            knowledgeSourceService.updateProcessingComplete(
                result.getKnowledgeSourceId(),
                result.getProcessingResult()
            );
            
            // 4. Send WebSocket notification to mobile app
            webSocketService.sendProcessingCompleteNotification(
                result.getUserId(),
                result.getKnowledgeSourceId()
            );
            
        } catch (Exception e) {
            log.error("Failed to handle processing success for job: {}", result.getJobId(), e);
        }
    }
    
    @RabbitListener(queues = "document.processing.error")
    public void handleDocumentProcessingError(ProcessingErrorResult error) {
        log.error("Processing failed for job: {} - {}", error.getJobId(), error.getError().getMessage());
        
        // Update knowledge source with error status
        knowledgeSourceService.updateProcessingError(
            error.getKnowledgeSourceId(),
            error.getError()
        );
        
        // Send error notification to mobile app
        webSocketService.sendProcessingErrorNotification(
            error.getUserId(),
            error.getKnowledgeSourceId(),
            error.getError()
        );
    }
    
    @RabbitListener(queues = "document.processing.status")
    public void handleProcessingStatusUpdate(ProcessingStatusUpdate status) {
        // Send real-time progress updates to mobile app
        webSocketService.sendProcessingStatusUpdate(
            status.getUserId(),
            status.getKnowledgeSourceId(),
            status.getProgress()
        );
    }
}
```

#### **Python Service: Message Consumer Implementation**
```python
import aio_pika
import asyncio
import json
from typing import Dict
import boto3

class MessageConsumer:
    def __init__(self):
        self.connection = None
        self.channel = None
        self.s3_client = boto3.client('s3')
        self.document_processor = DocumentProcessor()
        self.url_processor = URLProcessor()
    
    async def start_consuming(self):
        # Connect to RabbitMQ
        self.connection = await aio_pika.connect_robust(
            "amqp://guest:guest@localhost:5672/"
        )
        self.channel = await self.connection.channel()
        
        # Set up queues
        doc_queue = await self.channel.declare_queue(
            "document.processing.queue",
            durable=True
        )
        url_queue = await self.channel.declare_queue(
            "url.processing.queue", 
            durable=True
        )
        
        # Start consuming
        await doc_queue.consume(self.handle_document_message)
        await url_queue.consume(self.handle_url_message)
        
        print("🚀 Python service started consuming messages...")
        
    async def handle_document_message(self, message: aio_pika.IncomingMessage):
        async with message.process():
            try:
                data = json.loads(message.body.decode())
                print(f"📄 Processing document job: {data['jobId']}")
                
                # Send status update
                await self.send_status_update(data['jobId'], "STARTED", 0)
                
                # Process the document
                await self.document_processor.process_document(data)
                
            except Exception as e:
                print(f"❌ Error processing document: {str(e)}")
                await self.send_error_message(data['jobId'], str(e))
    
    async def handle_url_message(self, message: aio_pika.IncomingMessage):
        async with message.process():
            try:
                data = json.loads(message.body.decode())
                print(f"🌐 Processing URL job: {data['jobId']}")
                
                # Send status update
                await self.send_status_update(data['jobId'], "STARTED", 0)
                
                # Process the URL
                await self.url_processor.process_url(data)
                
            except Exception as e:
                print(f"❌ Error processing URL: {str(e)}")
                await self.send_error_message(data['jobId'], str(e))
    
    async def send_status_update(self, job_id: str, status: str, percentage: int):
        message = {
            "messageType": "PROCESSING_STATUS_UPDATE",
            "jobId": job_id,
            "status": status,
            "progress": {
                "percentage": percentage,
                "currentStep": f"Processing {status.lower()}",
                "timestamp": datetime.utcnow().isoformat()
            }
        }
        
        await self.publish_message("document.processing.status", message)
    
    async def send_success_message(self, job_id: str, result: Dict):
        message = {
            "messageType": "PROCESSING_COMPLETED",
            "jobId": job_id,
            "processingResult": result
        }
        
        await self.publish_message("document.processing.success", message)
    
    async def send_error_message(self, job_id: str, error: str):
        message = {
            "messageType": "PROCESSING_FAILED",
            "jobId": job_id,
            "error": {
                "errorType": "PROCESSING_ERROR",
                "message": error,
                "timestamp": datetime.utcnow().isoformat(),
                "retryable": True
            }
        }
        
        await self.publish_message("document.processing.error", message)
    
    async def publish_message(self, routing_key: str, message: Dict):
        await self.channel.default_exchange.publish(
            aio_pika.Message(
                json.dumps(message).encode(),
                content_type="application/json"
            ),
            routing_key=routing_key
        )

# Main execution
if __name__ == "__main__":
    consumer = MessageConsumer()
    asyncio.run(consumer.start_consuming())
```

### **⚡ Key Benefits of This Queue-Based Architecture**

1. **🔄 Async Processing**: Spring Boot APIs respond immediately, heavy processing happens in background
2. **📈 Scalability**: Can run multiple Python workers to process documents in parallel
3. **🛡️ Reliability**: Messages persisted in RabbitMQ, no data loss if services restart
4. **📱 Real-time Updates**: Mobile app gets live progress updates via WebSocket
5. **🔧 Error Handling**: Comprehensive error reporting with retry mechanisms
6. **💾 Cost Efficient**: S3 for document storage, processing only when needed
7. **🔒 Security**: Separate S3 access for each service with proper IAM roles
8. **📊 Monitoring**: Easy to monitor queue depths and processing performance

### **🚀 Processing Performance**

**Expected Processing Times:**
- **Small PDF (1-5 pages)**: 10-30 seconds
- **Medium PDF (10-50 pages)**: 1-3 minutes
- **Large PDF (100+ pages)**: 5-15 minutes
- **Web Articles**: 5-45 seconds
- **Complex Websites**: 1-5 minutes

**Throughput Capacity:**
- **Single Python Worker**: 10-20 documents/hour
- **Multiple Workers**: Scales linearly (3 workers = 30-60 docs/hour)
- **Queue Capacity**: Thousands of pending jobs
- **Storage**: Unlimited via S3

This architecture ensures your RAG application can handle document processing efficiently while maintaining excellent user experience through real-time status updates! 🎯🔄 Spring Boot → Python Service (RabbitMQ Messages)**

#### **Queue: `document.processing.queue`**

**Document Processing Request:**
```json
{
  "messageType": "PROCESS_DOCUMENT",
  "jobId": "job_12345",
  "userId": 1001,
  "projectId": 2001,
  "knowledgeSourceId": 3001,
  "documentInfo": {
    "fileName": "research_paper.pdf",
    "fileSize": 2048576,
    "mimeType": "application/pdf",
    "s3Location": {
      "bucket": "mobile-rag-documents",
      "key": "users/1001/projects/2001/documents/research_paper_12345.pdf",
      "region": "us-east-1"
    },
    "uploadedAt": "2025-06-22T10:30:00Z"
  },
  "processingOptions": {
    "chunkSize": 1000,
    "chunkOverlap": 200,
    "extractImages": false,
    "extractTables": true,
    "language": "en"
  },
  "userPlan": "PRO",
  "callbackConfig": {
    "successQueue": "document.processing.success",
    "errorQueue": "document.processing.error",
    "statusQueue": "document.processing.status"
  }
}
```

#### **Queue: `url.processing.queue`**

**Web URL Processing Request:**
```json
{
  "messageType": "PROCESS_WEB_URL",
  "jobId": "job_12346",
  "userId": 1001,
  "projectId": 2001,
  "knowledgeSourceId": 3002,
  "urlInfo": {
    "url": "https://example.com/article/ai-research",
    "title": "Latest AI Research Findings",
    "expectedContentType": "article",
    "crawlDepth": 1,
    "respectRobotsTxt": true
  },
  "processingOptions": {
    "chunkSize": 1000,
    "chunkOverlap": 200,
    "extractImages": false,
    "extractLinks": true,
    "language": "auto-detect"
  },
  "userPlan": "PRO",
  "callbackConfig": {
    "successQueue": "url.processing.success",
    "errorQueue": "url.processing.error",
    "statusQueue": "url.processing.status"
  }
}
```

### **🔄 Python Service → Spring Boot (RabbitMQ Messages)**

#### **Queue: `document.processing.status`**

**Processing Status Updates:**
```json
{
  "messageType": "PROCESSING_STATUS_UPDATE",
  "jobId": "job_12345",
  "status": "IN_PROGRESS",
  "progress": {
    "percentage": 65,
    "currentStep": "Extracting text from pages",
    "totalSteps": 4,
    "completedSteps": 2,
    "estimatedTimeRemaining": "2 minutes"
  },
  "processedData": {
    "totalPages": 45,
    "processedPages": 29,
    "extractedTextLength": 125000,
    "chunksGenerated": 85
  },
  "timestamp": "2025-06-22T10:32:15Z"
}
```

#### **Queue: `document.processing.success`**

**Successful Document Processing:**
```json
{
  "messageType": "PROCESSING_COMPLETED",
  "jobId": "job_12345",
  "knowledgeSourceId": 3001,
  "processingResult": {
    "status": "SUCCESS",
    "processedAt": "2025-06-22T10:35:00Z",
    "processingTimeSeconds": 180,
    "extractedContent": {
      "totalTextLength": 156000,
      "language": "en",
      "chunksCreated": 95,
      "metadata": {
        "title": "AI Research Paper on Neural Networks",
        "author": "Dr. Smith et al.",
        "publicationDate": "2024-12-15",
        "keywords": ["neural networks", "machine learning", "AI"],
        "documentType": "research_paper"
      }
    },
    "chunks": [
      {
        "chunkId": "chunk_1",
        "content": "Introduction to Neural Networks: Neural networks are computational models...",
        "chunkIndex": 0,
        "startPage": 1,
        "endPage": 1,
        "wordCount": 245,
        "metadata": {
          "section": "Introduction",
          "pageNumber": 1
        }
      },
      {
        "chunkId": "chunk_2",
        "content": "Deep Learning Fundamentals: Deep learning represents a subset...",
        "chunkIndex": 1,
        "startPage": 2,
        "endPage": 3,
        "wordCount": 289,
        "metadata": {
          "section": "Background",
          "pageNumber": 2
        }
      }
    ],
    "extractedAssets": {
      "images": [
        {
          "fileName": "figure_1_neural_network_diagram.png",
          "s3Location": "users/1001/projects/2001/assets/figure_1_12345.png",
          "caption": "Basic neural network architecture",
          "pageNumber": 5
        }
      ],
      "tables": [
        {
          "tableId": "table_1",
          "content": "| Model | Accuracy | Performance |\n|-------|----------|-------------|...",
          "caption": "Comparison of different neural network models",
          "pageNumber": 8
        }
      ]
    },
    "statistics": {
      "totalChunks": 95,
      "averageChunkLength": 985,
      "totalImages": 12,
      "totalTables": 6,
      "readabilityScore": 7.8
    }
  }
}
```

#### **Queue: `url.processing.success`**

**Successful Web URL Processing:**
```json
{
  "messageType": "URL_PROCESSING_COMPLETED",
  "jobId": "job_12346",
  "knowledgeSourceId": 3002,
  "processingResult": {
    "status": "SUCCESS",
    "processedAt": "2025-06-22T10:25:00Z",
    "processingTimeSeconds": 45,
    "crawlResult": {
      "originalUrl": "https://example.com/article/ai-research",
      "finalUrl": "https://example.com/article/ai-research",
      "title": "Latest AI Research Findings",
      "contentType": "article",
      "language": "en",
      "publishDate": "2025-06-20",
      "author": "Jane Doe",
      "contentLength": 8500,
      "cleanedContent": "Latest AI Research Findings\n\nThe field of artificial intelligence...",
      "chunksCreated": 12
    },
    "chunks": [
      {
        "chunkId": "chunk_web_1",
        "content": "Latest AI Research Findings. The field of artificial intelligence has seen remarkable advances...",
        "chunkIndex": 0,
        "sourceSection": "introduction",
        "wordCount": 234,
        "metadata": {
          "url": "https://example.com/article/ai-research",
          "section": "introduction",
          "extractedAt": "2025-06-22T10:25:00Z"
        }
      }
    ],
    "extractedAssets": {
      "links": [
        {
          "url": "https://example.com/related-research",
          "text": "Related Research Papers",
          "context": "For more information, see our related research papers"
        }
      ],
      "images": [
        {
          "url": "https://example.com/images/ai-diagram.png",
          "alt": "AI Architecture Diagram",
          "caption": "Overview of the proposed AI architecture"
        }
      ]
    },
    "metadata": {
      "domain": "example.com",
      "crawledAt": "2025-06-22T10:25:00Z",
      "responseTime": 450,
      "statusCode": 200,
      "contentEncoding": "gzip"
    }
  }
}
```

#### **Queue: `document.processing.error` / `url.processing.error`**

**Error Processing Result:**
```json
{
  "messageType": "PROCESSING_FAILED",
  "jobId": "job_12347",
  "knowledgeSourceId": 3003,
  "error": {
    "errorType": "EXTRACTION_FAILED",
    "errorCode": "PDF_CORRUPTED",
    "message": "Unable to extract text from PDF: file appears to be corrupted",
    "details": {
      "fileName": "corrupted_document.pdf",
      "fileSize": 1024000,
      "pagesAttempted": 0,
      "specificError": "PyPDF2.PdfReadError: File has not been decrypted"
    },
    "timestamp": "2025-06-22T10:28:00Z",
    "retryable": false,
    "suggestedAction": "Re-upload the document or try a different file format"
  },
  "partialResult": null
}
```

### **🔧 Python Service Processing Implementation**

#### **Document Processing (PDF, DOCX, etc.):**
```python
import boto3
import asyncio
from typing import Dict, List
import aio_pika

class DocumentProcessor:
    def __init__(self):
        self.s3_client = boto3.client('s3')
        
    async def process_document(self, message: Dict):
        job_id = message['jobId']
        s3_info = message['documentInfo']['s3Location']
        
        try:
            # 1. Download from S3
            local_path = f"/tmp/{job_id}_{s3_info['key'].split('/')[-1]}"
            self.s3_client.download_file(
                s3_info['bucket'], 
                s3_info['key'], 
                local_path
            )
            
            # 2. Extract content based on MIME type
            mime_type = message['documentInfo']['mimeType']
            if mime_type == "application/pdf":
                content = await self.extract_pdf_content(local_path)
            elif mime_type.startswith("application/vnd.openxmlformats"):
                content = await self.extract_docx_content(local_path)
            
            # 3. Clean and preprocess
            cleaned_content = self.clean_text(content['raw_text'])
            metadata = content['metadata']
            
            # 4. Chunk the content
            chunks = self.chunk_text(
                cleaned_content,
                chunk_size=message['processingOptions']['chunkSize'],
                overlap=message['processingOptions']['chunkOverlap']
            )
            
            # 5. Enhance chunks with metadata
            enhanced_chunks = self.add_chunk_metadata(chunks, metadata)
            
            # 6. Publish success result
            await self.publish_success_message(job_id, enhanced_chunks, metadata)
            
        except Exception as e:
            await self.publish_error_message(job_id, str(e))
    
    async def extract_pdf_content(self, file_path: str) -> Dict:
        import PyPDF2
        content = {"raw_text": "", "metadata": {}}
        
        with open(file_path, 'rb') as file:
            pdf_reader = PyPDF2.PdfReader(file)
            
            # Extract metadata
            content["metadata"] = {
                "title": pdf_reader.metadata.get('/Title', ''),
                "author": pdf_reader.metadata.get('/Author', ''),
                "pages": len(pdf_reader.pages)
            }
            
            # Extract text from all pages
            for page_num, page in enumerate(pdf_reader.pages):
                page_text = page.extract_text()
                content["raw_text"] += f"\n[Page {page_num + 1}]\n{page_text}"
        
        return content
```

#### **Web URL Processing with Crawl4AI:**
```python
from crawl4ai import AsyncWebCrawler
from crawl4ai.extraction_strategy import JsonCssExtractionStrategy

class URLProcessor:
    async def process_url(self, message: Dict):
        job_id = message['jobId']
        url_info = message['urlInfo']
        
        try:
            # 1. Initialize Crawl4AI
            async with AsyncWebCrawler() as crawler:
                # 2. Configure extraction strategy
                extraction_strategy = JsonCssExtractionStrategy({
                    "title": "h1, title",
                    "content": "article, .content, main, .post-content",
                    "author": ".author, [rel='author'], .byline",
                    "publish_date": "time, .date, .published"
                })
                
                # 3. Crawl the URL
                result = await crawler.arun(
                    url=url_info['url'],
                    extraction_strategy=extraction_strategy,
                    bypass_cache=True,
                    word_count_threshold=10
                )
                
                # 4. Clean and structure content
                cleaned_content = self.clean_web_content(result.markdown)
                metadata = self.extract_web_metadata(result)
                
                # 5. Chunk the content
                chunks = self.chunk_web_content(
                    cleaned_content,
                    chunk_size=message['processingOptions']['chunkSize'],
                    overlap=message['processingOptions']['chunkOverlap']
                )
                
                # 6. Publish success result
                await self.publish_url_success_message(job_id, chunks, metadata)
                
        except Exception as e:
            await self.publish_error_message(job_id, str(e))
    
    def clean_web_content(self, markdown_content: str) -> str:
        # Remove excessive whitespace, clean formatting
        import re
        cleaned = re.sub(r'\n\s*\n', '\n\n', markdown_content)
        cleaned = re.sub(r'[^\S\n]+', ' ', cleaned)
        return cleaned.strip()
```

### **# Mobile RAG Application - High Level Design (HLD)

## 🎯 Executive Summary

This document outlines the complete architecture for a **mobile-first RAG (Retrieval Augmented Generation) application** that enables users to create knowledge-based projects with personalized AI chat sessions. The system implements project-based knowledge management with secure authentication and real-time processing capabilities.

## 📱 Application Overview

### Core Concept
- **Mobile-First**: Native iOS/Android app with RESTful backend
- **Project-Based Knowledge**: Users create projects containing knowledge sources and custom instructions
- **Simple Subscription Model**: Free and Pro plans for individual users (no enterprise features)
- **Persistent Chat Sessions**: Multiple chat sessions per project - **ALL CONVERSATIONS ARE PERMANENTLY STORED**
- **Session Inheritance**: Each chat session inherits project knowledge and instructions
- **Complete Chat History**: Users can access all their previous conversations anytime
- **Personalized AI**: LLM addresses users by their preferred name with project-specific context
- **Side Project Focus**: Designed for individual productivity and personal knowledge management

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        MOBILE APPLICATION                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   Auth Module   │  │ Project Manager │  │  Chat Interface │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────┬───────────────┬───────────────┬───────────────┘
                  │               │               │
              JWT + Redis     REST APIs      WebSocket/SSE
                  │               │               │
┌─────────────────▼───────────────▼───────────────▼───────────────┐
│                     SPRING BOOT BACKEND                         │
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ │
│ │    Auth     │ │   Project   │ │  Enhanced   │ │   Document  │ │
│ │  Service    │ │  Service    │ │ Chat Service│ │  Service    │ │
│ │             │ │             │ │ + Memory AI │ │             │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ │
└─────────┬───────────────┬───────────────┬───────────────┬───────┘
          │               │               │               │
     JWT/Redis       PostgreSQL      RabbitMQ         OpenAI
          │               │               │               │
┌─────────▼─┐    ┌────────▼────────┐ ┌────▼────┐ ┌─────────▼─────────┐
│   Redis   │    │   PostgreSQL    │ │RabbitMQ │ │ Python Document   │
│  (Cache)  │    │  (Relational)   │ │Message  │ │ Processing Service│
└───────────┘    └─────────────────┘ │  Bus    │ └─────────────────┬─┘
                           │         │         │                   │
                           │         └─────────┘                   │
                           │         ┌─────────┐                   │
                           │         │   AWS   │                   │
                           │         │   S3    │◄──────────────────┘
                           │         │Document │ File Storage &
                           │         │Storage  │ Retrieval
                           │         └─────────┘
                           │                  │
                    ┌──────▼──────┐    ┌─────▼─────┐
                    │ Dual Qdrant │◄───│  OpenAI   │
                    │   Vectors   │    │Embeddings │ 
                    │             │    └───────────┘
                    │┌───────────┐│
                    ││Knowledge  ││ ← RAG Documents & Web Content
                    ││Collection ││
                    │└───────────┘│
                    │┌───────────┐│
                    ││Semantic   ││ ← Chat Memory for Intelligent Context
                    ││Chat Memory││
                    │└───────────┘│
                    └─────────────┘
```

## 🔐 Authentication Architecture

### Supported Methods
1. **Email + OTP**: Phone number verification with SMS/Email OTP
2. **Google OAuth**: Gmail integration for quick signup

### Security Flow
```mermaid
sequenceDiagram
    participant M as Mobile App
    participant S as Spring Boot
    participant R as Redis
    participant E as Email/SMS Service
    participant G as Google OAuth

    Note over M,G: Email + OTP Flow
    M->>S: POST /auth/signup (email)
    S->>E: Send OTP
    S->>R: Store OTP (5min TTL)
    M->>S: POST /auth/verify-otp (email, otp)
    S->>R: Validate OTP
    S->>S: Create User
    S->>R: Store JWT (24h TTL)
    S-->>M: JWT Token + User Data

    Note over M,G: Google OAuth Flow  
    M->>G: Google Sign-In
    G-->>M: Google Token
    M->>S: POST /auth/google (google_token)
    S->>G: Verify Token
    S->>S: Create/Update User
    S->>R: Store JWT (24h TTL)
    S-->>M: JWT Token + User Data
```

## 📊 Data Model & Database Design

### PostgreSQL Schema

```sql
-- Users table (Enhanced with Subscription Management)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    preferred_name VARCHAR(100), -- What LLM calls the user
    auth_provider VARCHAR(50) NOT NULL, -- 'EMAIL_OTP' or 'GOOGLE'
    google_id VARCHAR(255) UNIQUE,
    profile_image_url TEXT,
    
    -- Subscription Management
    current_plan VARCHAR(50) DEFAULT 'FREE', -- 'FREE', 'PRO', 'ENTERPRISE'
    plan_started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    plan_expires_at TIMESTAMP, -- NULL for FREE plan
    subscription_status VARCHAR(50) DEFAULT 'ACTIVE', -- 'ACTIVE', 'CANCELLED', 'EXPIRED', 'PAST_DUE'
    stripe_customer_id VARCHAR(255), -- For payment processing
    
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_users_plan (current_plan),
    INDEX idx_users_subscription_status (subscription_status),
    INDEX idx_users_plan_expires (plan_expires_at)
);

-- Subscription plans configuration
CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    plan_name VARCHAR(50) UNIQUE NOT NULL, -- 'FREE', 'PRO', 'ENTERPRISE'
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    price_monthly DECIMAL(10,2) DEFAULT 0.00,
    price_yearly DECIMAL(10,2) DEFAULT 0.00,
    
    -- Plan Limits
    max_projects INTEGER DEFAULT 1,
    max_knowledge_sources_per_project INTEGER DEFAULT 3,
    max_file_size_mb INTEGER DEFAULT 10,
    max_monthly_messages INTEGER DEFAULT 100,
    max_storage_gb DECIMAL(5,2) DEFAULT 0.5,
    
    -- Feature Flags
    can_use_web_urls BOOLEAN DEFAULT false,
    can_export_conversations BOOLEAN DEFAULT false,
    can_use_advanced_ai BOOLEAN DEFAULT false,
    has_priority_support BOOLEAN DEFAULT false,
    can_use_semantic_search BOOLEAN DEFAULT false,
    
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert simplified plans for individual users only
INSERT INTO subscription_plans (plan_name, display_name, description, price_monthly, price_yearly, max_projects, max_knowledge_sources_per_project, max_file_size_mb, max_monthly_messages, max_storage_gb, can_use_web_urls, can_export_conversations, can_use_advanced_ai, has_priority_support, can_use_semantic_search) VALUES
('FREE', 'Free Plan', 'Perfect for getting started with AI assistance', 0.00, 0.00, 3, 5, 10, 100, 0.5, false, false, false, false, false),
('PRO', 'Pro Plan', 'Advanced features for power users', 9.99, 99.99, 25, 100, 50, 1000, 5.0, true, true, true, true, true);

-- Usage tracking table
CREATE TABLE user_usage (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_month DATE NOT NULL, -- First day of the month (e.g., 2025-01-01)
    
    -- Current Usage Counts
    projects_created INTEGER DEFAULT 0,
    knowledge_sources_added INTEGER DEFAULT 0,
    messages_sent INTEGER DEFAULT 0,
    storage_used_mb DECIMAL(10,2) DEFAULT 0.00,
    ai_tokens_used INTEGER DEFAULT 0,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE KEY unique_user_month (user_id, usage_month),
    INDEX idx_user_usage_month (usage_month),
    INDEX idx_user_usage_user_id (user_id)
);

-- Subscription history for audit
CREATE TABLE subscription_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    previous_plan VARCHAR(50),
    new_plan VARCHAR(50) NOT NULL,
    change_reason VARCHAR(100), -- 'UPGRADE', 'DOWNGRADE', 'CANCELLATION', 'RENEWAL', 'EXPIRED'
    effective_date TIMESTAMP NOT NULL,
    stripe_transaction_id VARCHAR(255),
    amount_paid DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_subscription_history_user_id (user_id),
    INDEX idx_subscription_history_effective_date (effective_date)
);

-- Projects table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(300) NOT NULL,
    description TEXT,
    instructions TEXT NOT NULL, -- Custom instructions for this project
    system_prompt TEXT, -- Generated system prompt combining instructions
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_projects_user_id (user_id),
    INDEX idx_projects_active (is_active)
);

-- Knowledge sources for projects
CREATE TABLE knowledge_sources (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_type VARCHAR(50) NOT NULL, -- 'DOCUMENT', 'WEB_URL', 'TEXT'
    source_name VARCHAR(500) NOT NULL,
    source_url TEXT, -- For web URLs
    file_path TEXT, -- For uploaded documents
    content_preview TEXT, -- First 500 chars for display
    processing_status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    chunk_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    
    INDEX idx_knowledge_sources_project_id (project_id),
    INDEX idx_knowledge_sources_status (processing_status)
);

-- Chat sessions within projects (PERMANENT STORAGE)
CREATE TABLE chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    session_name VARCHAR(300), -- User can name their chat sessions
    last_message_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true, -- For UI organization, not deletion
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_chat_sessions_project_id (project_id),
    INDEX idx_chat_sessions_active (is_active),
    INDEX idx_chat_sessions_last_message (last_message_at DESC)
);

-- NOTE: Chat sessions are NEVER deleted unless the entire project is deleted
-- is_active flag is only for UI organization (archived vs active sessions)

-- Chat messages (COMPLETE PERMANENT HISTORY + VECTOR INTEGRATION)
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    message_type VARCHAR(50) NOT NULL, -- 'USER', 'ASSISTANT', 'SYSTEM'
    content TEXT NOT NULL,
    vector_id VARCHAR(255), -- Reference to Qdrant vector document
    conversation_group_id VARCHAR(255), -- Groups related Q&A exchanges
    metadata JSONB, -- Store tokens used, sources referenced, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_chat_messages_session_id (session_id),
    INDEX idx_chat_messages_created_at (created_at DESC),
    INDEX idx_chat_messages_vector_id (vector_id),
    INDEX idx_chat_messages_conversation_group (conversation_group_id)
);

-- NOTE: ALL messages are stored permanently
-- Users can browse their complete conversation history
-- No automatic deletion or cleanup of chat data
-- Vector integration enables semantic search across all conversations

-- Vector memory tracking table
CREATE TABLE vector_chat_memory (
    id BIGSERIAL PRIMARY KEY,
    chat_message_id BIGINT NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    vector_id VARCHAR(255) NOT NULL,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    qdrant_collection VARCHAR(100) DEFAULT 'chat_memory',
    embedding_created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    similarity_threshold FLOAT DEFAULT 0.7,
    
    INDEX idx_vector_chat_memory_project (project_id),
    INDEX idx_vector_chat_memory_vector_id (vector_id),
    INDEX idx_vector_chat_memory_message (chat_message_id),
    
    UNIQUE KEY unique_message_vector (chat_message_id, vector_id)
);

-- Document processing jobs
CREATE TABLE processing_jobs (
    id BIGSERIAL PRIMARY KEY,
    knowledge_source_id BIGINT NOT NULL REFERENCES knowledge_sources(id) ON DELETE CASCADE,
    job_type VARCHAR(50) NOT NULL, -- 'DOCUMENT_PARSE', 'WEB_SCRAPE', 'EMBEDDING_GENERATE'
    status VARCHAR(50) DEFAULT 'PENDING',
    progress_percentage INTEGER DEFAULT 0,
    result_data JSONB,
    error_details TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_processing_jobs_source_id (knowledge_source_id),
    INDEX idx_processing_jobs_status (status)
);

-- User sessions for analytics
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(500) NOT NULL,
    device_info JSONB,
    ip_address INET,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_sessions_token (session_token),
    INDEX idx_user_sessions_user_id (user_id),
    INDEX idx_user_sessions_expires (expires_at)
);
```

### Qdrant Vector Database Schema (Dual Collections)

```yaml
# Collection 1: Project Knowledge Base (RAG Documents)
Collection: project_knowledge
  vector_size: 1536 # OpenAI text-embedding-ada-002
  distance: Cosine
  
Payload Schema:
  project_id: integer
  knowledge_source_id: integer
  chunk_id: string
  content: string
  chunk_index: integer
  metadata:
    source_name: string
    source_type: string
    source_url: string (optional)
    processed_at: timestamp
    user_id: integer

# Collection 2: Semantic Chat Memory (Intelligent Conversation Context)
Collection: chat_memory
  vector_size: 1536 # OpenAI text-embedding-ada-002
  distance: Cosine
  
Payload Schema:
  project_id: integer
  session_id: integer
  user_id: integer
  message_id: integer
  message_type: string # 'USER', 'ASSISTANT', 'SYSTEM'
  content: string
  conversation_group_id: string # Groups related exchanges (Q&A pairs)
  timestamp: timestamp
  metadata:
    session_name: string
    tokens_used: integer (optional)
    processing_time: float (optional)
```

### Redis Schema

```yaml
Key Patterns:
  # JWT tokens
  jwt:user:{user_id}:{token_hash}: user_session_data (24h TTL)
  
  # OTP verification
  otp:{email}:{otp_code}: verification_data (5min TTL)
  
  # Rate limiting
  rate_limit:auth:{ip}: attempt_count (1h TTL)
  rate_limit:api:{user_id}: request_count (1h TTL)
  
  # Performance caching only (all chat data stored permanently in PostgreSQL)
  chat:session:{session_id}:metadata: session_metadata (30min TTL) # For quick access
  project:{project_id}:summary: project_summary (15min TTL) # For quick access
  
  # User activity tracking
  user:{user_id}:last_active: timestamp (1h TTL)
  
  # Temporary processing status
  processing:{job_id}: status_data (1h TTL)
```

## 🎛️ Core Services Architecture

### 1. Authentication Service
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/signup/email")
    public ResponseEntity<AuthResponse> signupWithEmail(@RequestBody EmailSignupRequest request);
    
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOTP(@RequestBody OTPVerificationRequest request);
    
    @PostMapping("/signup/google")
    public ResponseEntity<AuthResponse> signupWithGoogle(@RequestBody GoogleAuthRequest request);
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request);
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token);
}
```

### 2. Subscription Management Service (NEW)
```java
@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private UsageLimitService usageLimitService;
    
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlan>> getAvailablePlans();
    
    @GetMapping("/current")
    public ResponseEntity<UserSubscriptionDetails> getCurrentSubscription();
    
    @GetMapping("/usage")
    public ResponseEntity<UsageDetails> getCurrentUsage() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(usageLimitService.getCurrentUsage(userId));
    }
    
    @PostMapping("/upgrade")
    public ResponseEntity<UpgradeResponse> upgradeToPro(@RequestBody UpgradeRequest request) {
        // Integrate with Stripe for payment processing
        return ResponseEntity.ok(subscriptionService.upgradeToPro(getCurrentUserId(), request));
    }
    
    @PostMapping("/downgrade")
    public ResponseEntity<Void> downgradeToFree() {
        subscriptionService.downgradeToFree(getCurrentUserId());
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/limits")
    public ResponseEntity<PlanLimits> getCurrentLimits() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(usageLimitService.getUserLimits(userId));
    }
    
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelSubscription() {
        subscriptionService.cancelSubscription(getCurrentUserId());
        return ResponseEntity.ok().build();
    }
}
```

### 3. Enhanced Project Management Service (With Usage Limits)
```java
@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    
    @Autowired
    private UsageLimitService usageLimitService;
    
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody CreateProjectRequest request) {
        Long userId = getCurrentUserId();
        
        // Check plan limits before creating project
        if (!usageLimitService.canCreateProject(userId)) {
            throw new PlanLimitExceededException("Project limit exceeded for current plan");
        }
        
        ProjectResponse project = projectService.createProject(userId, request);
        usageLimitService.incrementProjectCount(userId);
        
        return ResponseEntity.ok(project);
    }
    
    @GetMapping
    public ResponseEntity<List<ProjectSummary>> getUserProjects();
    
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetails> getProjectDetails(@PathVariable Long projectId);
    
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long projectId, @RequestBody UpdateProjectRequest request);
    
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(getCurrentUserId(), projectId);
        usageLimitService.decrementProjectCount(getCurrentUserId());
        return ResponseEntity.ok().build();
    }
    
    // Knowledge Source Management with Limits
    @PostMapping("/{projectId}/knowledge-sources")
    public ResponseEntity<KnowledgeSourceResponse> addKnowledgeSource(
            @PathVariable Long projectId, 
            @RequestBody AddKnowledgeSourceRequest request) {
        
        Long userId = getCurrentUserId();
        
        // Check plan limits
        if (!usageLimitService.canAddKnowledgeSource(userId, projectId)) {
            throw new PlanLimitExceededException("Knowledge source limit exceeded for current plan");
        }
        
        // Check file size limits for uploads
        if (request.getSourceType() == SourceType.DOCUMENT && 
            !usageLimitService.isFileSizeAllowed(userId, request.getFileSize())) {
            throw new PlanLimitExceededException("File size exceeds plan limit");
        }
        
        // Check web URL feature availability
        if (request.getSourceType() == SourceType.WEB_URL && 
            !usageLimitService.canUseWebUrls(userId)) {
            throw new FeatureNotAvailableException("Web URL processing requires Pro plan");
        }
        
        KnowledgeSourceResponse response = projectService.addKnowledgeSource(projectId, request);
        usageLimitService.incrementKnowledgeSourceCount(userId, projectId);
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{projectId}/knowledge-sources/{sourceId}")
    public ResponseEntity<Void> removeKnowledgeSource(@PathVariable Long projectId, @PathVariable Long sourceId) {
        projectService.removeKnowledgeSource(getCurrentUserId(), projectId, sourceId);
        usageLimitService.decrementKnowledgeSourceCount(getCurrentUserId(), projectId);
        return ResponseEntity.ok().build();
    }
}
```

### 4. Enhanced Chat Service (With Message Limits & Plan Features)
```java
@RestController
@RequestMapping("/api/chat")
public class EnhancedChatController {
    
    @Autowired
    private VectorStore knowledgeVectorStore; // RAG documents
    
    @Autowired 
    private VectorStore chatMemoryVectorStore; // Semantic chat memory
    
    @Autowired
    private ChatMemoryRepository traditionalChatMemory; // Sequential history
    
    @Autowired
    private UsageLimitService usageLimitService;
    
    @PostMapping("/sessions")
    public ResponseEntity<ChatSession> createChatSession(@RequestBody CreateChatSessionRequest request);
    
    @GetMapping("/projects/{projectId}/sessions")
    public ResponseEntity<List<ChatSession>> getProjectChatSessions(@PathVariable Long projectId);
    
    @GetMapping("/projects/{projectId}/sessions/all") 
    public ResponseEntity<List<ChatSession>> getAllProjectSessions(@PathVariable Long projectId);
    
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessageWithAI(@PathVariable Long sessionId, @RequestBody ChatMessageRequest request) {
        Long userId = getCurrentUserId();
        
        // Check message limits
        if (!usageLimitService.canSendMessage(userId)) {
            throw new PlanLimitExceededException("Monthly message limit exceeded. Upgrade to Pro for unlimited messages.");
        }
        
        // Create project-specific ChatClient with plan-appropriate features
        ChatClient projectChatClient = createProjectChatClient(request.getProjectId(), userId);
        
        String response = projectChatClient.prompt()
            .advisors(advisorSpec -> advisorSpec
                .param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId.toString())
                .param("project_id", request.getProjectId().toString()))
            .user(request.getMessage())
            .call()
            .content();
            
        // Track usage
        usageLimitService.incrementMessageCount(userId);
        
        return ResponseEntity.ok(new ChatMessageResponse(response));
    }
    
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessage>> getSessionMessages(@PathVariable Long sessionId, @RequestParam(defaultValue = "0") int page);
    
    @GetMapping("/sessions/{sessionId}/export")
    public ResponseEntity<String> exportConversation(@PathVariable Long sessionId) {
        Long userId = getCurrentUserId();
        
        // Check if user can export conversations
        if (!usageLimitService.canExportConversations(userId)) {
            throw new FeatureNotAvailableException("Conversation export requires Pro plan");
        }
        
        return ResponseEntity.ok(chatService.exportConversation(sessionId));
    }
    
    @GetMapping("/projects/{projectId}/search-conversations")
    public ResponseEntity<List<ChatMessage>> searchConversationsSemanticaly(@PathVariable Long projectId, @RequestParam String query) {
        Long userId = getCurrentUserId();
        
        // Check if user can use semantic search
        if (!usageLimitService.canUseSemanticSearch(userId)) {
            throw new FeatureNotAvailableException("Semantic conversation search requires Pro plan");
        }
        
        return ResponseEntity.ok(chatSemanticSearchService.searchSimilarConversations(projectId, query));
    }
    
    @PutMapping("/sessions/{sessionId}/archive")
    public ResponseEntity<Void> archiveSession(@PathVariable Long sessionId);
    
    @PutMapping("/sessions/{sessionId}/rename")
    public ResponseEntity<Void> renameSession(@PathVariable Long sessionId, @RequestBody RenameSessionRequest request);
    
    // Plan-aware ChatClient creation
    private ChatClient createProjectChatClient(Long projectId, Long userId) {
        List<Advisor> advisors = new ArrayList<>();
        
        // Layer 1: Knowledge Base RAG (Always available)
        advisors.add(RetrievalAugmentationAdvisor.builder()
            .documentRetriever(VectorStoreDocumentRetriever.builder()
                .vectorStore(knowledgeVectorStore)
                .searchRequest(SearchRequest.builder()
                    .filterExpression("project_id == " + projectId)
                    .topK(5)
                    .similarityThreshold(0.7)
                    .build())
                .build())
            .build());
        
        // Layer 2: Semantic Chat Memory (Pro plan only)
        if (usageLimitService.canUseSemanticSearch(userId)) {
            advisors.add(VectorStoreChatMemoryAdvisor.builder()
                .vectorStore(chatMemoryVectorStore)
                .filterExpression("project_id == " + projectId)
                .chatHistoryWindowSize(20) // Top 20 semantically relevant conversations
                .order(100)
                .build());
        }
        
        // Layer 3: Sequential Memory (Always available, but limited window for free users)
        int memoryWindow = usageLimitService.canUseAdvancedAI(userId) ? 10 : 5;
        advisors.add(MessageChatMemoryAdvisor.builder(traditionalChatMemory)
            .chatHistoryWindowSize(memoryWindow)
            .order(200)
            .build());
        
        return ChatClient.builder(chatModel)
            .defaultAdvisors(advisors)
            .build();
    }
    
    // NOTE: No DELETE endpoint - conversations are permanent
}
```

### 5. Document Processing Service (With File Size Limits)
```java
@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    
    @Autowired
    private UsageLimitService usageLimitService;
    
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file, 
            @RequestParam("projectId") Long projectId) {
        
        Long userId = getCurrentUserId();
        
        // Check file size limits
        if (!usageLimitService.isFileSizeAllowed(userId, file.getSize())) {
            throw new PlanLimitExceededException("File size exceeds plan limit. Current limit: " + 
                usageLimitService.getMaxFileSizeMB(userId) + "MB");
        }
        
        // Check storage limits
        if (!usageLimitService.hasStorageCapacity(userId, file.getSize())) {
            throw new PlanLimitExceededException("Storage limit exceeded. Upgrade to Pro for more storage.");
        }
        
        DocumentUploadResponse response = documentService.uploadDocument(file, projectId);
        usageLimitService.addStorageUsage(userId, file.getSize());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/url")
    public ResponseEntity<URLProcessingResponse> processURL(@RequestBody ProcessURLRequest request) {
        Long userId = getCurrentUserId();
        
        // Check if user can process URLs
        if (!usageLimitService.canUseWebUrls(userId)) {
            throw new FeatureNotAvailableException("Web URL processing requires Pro plan");
        }
        
        return ResponseEntity.ok(documentService.processURL(request));
    }
    
    @GetMapping("/processing-status/{jobId}")
    public ResponseEntity<ProcessingStatus> getProcessingStatus(@PathVariable Long jobId);
}
```

### 6. Usage Limit Service (NEW)
```java
@Service
public class UsageLimitService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private SubscriptionPlanRepository planRepository;
    
    @Autowired
    private UserUsageRepository usageRepository;
    
    public boolean canCreateProject(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        
        int currentProjects = projectRepository.countByUserId(userId);
        return currentProjects < plan.getMaxProjects();
    }
    
    public boolean canAddKnowledgeSource(Long userId, Long projectId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        
        int currentSources = knowledgeSourceRepository.countByProjectId(projectId);
        return currentSources < plan.getMaxKnowledgeSourcesPerProject();
    }
    
    public boolean canSendMessage(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        
        if (plan.getMaxMonthlyMessages() == -1) return true; // Unlimited for Pro
        
        UserUsage usage = getCurrentMonthUsage(userId);
        return usage.getMessagesSent() < plan.getMaxMonthlyMessages();
    }
    
    public boolean isFileSizeAllowed(Long userId, long fileSizeBytes) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        
        long fileSizeMB = fileSizeBytes / (1024 * 1024);
        return fileSizeMB <= plan.getMaxFileSizeMb();
    }
    
    public boolean canUseWebUrls(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        return plan.getCanUseWebUrls();
    }
    
    public boolean canExportConversations(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        return plan.getCanExportConversations();
    }
    
    public boolean canUseSemanticSearch(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        return plan.getCanUseSemanticSearch();
    }
    
    public boolean canUseAdvancedAI(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        return plan.getCanUseAdvancedAi();
    }
    
    public PlanLimits getUserLimits(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findByPlanName(user.getCurrentPlan()).orElseThrow();
        UserUsage usage = getCurrentMonthUsage(userId);
        
        return PlanLimits.builder()
            .planName(plan.getPlanName())
            .maxProjects(plan.getMaxProjects())
            .currentProjects(projectRepository.countByUserId(userId))
            .maxKnowledgeSourcesPerProject(plan.getMaxKnowledgeSourcesPerProject())
            .maxMonthlyMessages(plan.getMaxMonthlyMessages())
            .currentMonthlyMessages(usage.getMessagesSent())
            .maxFileSizeMB(plan.getMaxFileSizeMb())
            .maxStorageGB(plan.getMaxStorageGb())
            .currentStorageGB(usage.getStorageUsedMb() / 1024.0)
            .features(PlanFeatures.builder()
                .canUseWebUrls(plan.getCanUseWebUrls())
                .canExportConversations(plan.getCanExportConversations())
                .canUseAdvancedAI(plan.getCanUseAdvancedAi())
                .canUseSemanticSearch(plan.getCanUseSemanticSearch())
                .hasPrioritySupport(plan.getHasPrioritySupport())
                .build())
            .build();
    }
    
    private UserUsage getCurrentMonthUsage(Long userId) {
        Date currentMonth = Date.from(YearMonth.now().atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        return usageRepository.findByUserIdAndUsageMonth(userId, currentMonth)
            .orElseGet(() -> {
                UserUsage newUsage = new UserUsage();
                newUsage.setUserId(userId);
                newUsage.setUsageMonth(currentMonth);
                return usageRepository.save(newUsage);
            });
    }
    
    // Usage tracking methods
    public void incrementProjectCount(Long userId) { /* Implementation */ }
    public void incrementKnowledgeSourceCount(Long userId, Long projectId) { /* Implementation */ }
    public void incrementMessageCount(Long userId) { /* Implementation */ }
    public void addStorageUsage(Long userId, long bytes) { /* Implementation */ }
}
```

### 7. Chat Memory Vectorization Service (Enhanced with Plan Features)
```java
@Component
public class ChatMemoryVectorizationService {
    
    @Autowired
    private VectorStore chatMemoryVectorStore;
    
    @Autowired
    private UsageLimitService usageLimitService;
    
    @EventListener
    @Async
    public void onChatMessageSaved(ChatMessageSavedEvent event) {
        ChatMessage message = event.getChatMessage();
        Long userId = message.getSession().getProject().getUser().getId();
        
        // Skip vectorization for free users (no semantic search)
        if (!usageLimitService.canUseSemanticSearch(userId)) {
            return;
        }
        
        // Skip system messages or very short messages
        if (message.getMessageType() == MessageType.SYSTEM || 
            message.getContent().length() < 10) {
            return;
        }
        
        try {
            // Create conversation group ID for Q&A pairing
            String conversationGroupId = generateConversationGroupId(message);
            
            // Create document for vector storage
            Document chatDocument = new Document(
                message.getContent(),
                Map.of(
                    "project_id", message.getSession().getProject().getId(),
                    "session_id", message.getSession().getId(),
                    "user_id", userId,
                    "message_id", message.getId(),
                    "message_type", message.getMessageType().toString(),
                    "conversation_group_id", conversationGroupId,
                    "timestamp", message.getCreatedAt().toString(),
                    "session_name", message.getSession().getSessionName(),
                    "user_plan", message.getSession().getProject().getUser().getCurrentPlan()
                )
            );
            
            // Store in vector database for semantic search (Pro users only)
            chatMemoryVectorStore.add(List.of(chatDocument));
            
            // Track in relational database
            VectorChatMemory vectorMemory = new VectorChatMemory();
            vectorMemory.setChatMessageId(message.getId());
            vectorMemory.setVectorId(chatDocument.getId());
            vectorMemory.setProjectId(message.getSession().getProject().getId());
            vectorChatMemoryRepository.save(vectorMemory);
            
            // Update original message with vector reference
            message.setVectorId(chatDocument.getId());
            message.setConversationGroupId(conversationGroupId);
            chatMessageRepository.save(message);
            
        } catch (Exception e) {
            log.error("Failed to vectorize chat message: " + message.getId(), e);
        }
    }
    
    // Rest of the implementation...
}
```

## 💾 Persistent Conversation Management

### **Complete Chat History Storage**
- **Never Lost**: All conversations are permanently stored in PostgreSQL
- **Always Accessible**: Users can browse their complete conversation history anytime
- **Cross-Session Context**: AI has access to full conversation history for better context
- **Search & Export**: Users can search across all conversations and export chat histories

### **Session Organization**
```
Project: "Marketing Strategy"
├── Session 1: "Q4 Campaign Ideas" (Created: Jan 15, 45 messages)
├── Session 2: "Competitor Analysis" (Created: Jan 18, 23 messages) 
├── Session 3: "Budget Planning" (Created: Jan 20, 67 messages)
└── Session 4: "Content Calendar" (Created: Jan 22, 12 messages) [ACTIVE]
```

### **Chat Session Lifecycle**
1. **Creation**: User creates a new chat session within a project
2. **Active Usage**: All messages are immediately stored to database
3. **Archiving**: User can archive sessions for organization (NOT deletion)
4. **Resumption**: User can resume any conversation at any time
5. **Permanence**: Conversations are only deleted if the entire project is deleted

### **Benefits of Persistent Storage**
- **Continuity**: Pick up conversations weeks later with full context
- **Knowledge Building**: Build upon previous discussions and insights
- **Reference Material**: Use past conversations as reference for new discussions
- **Analytics**: Track conversation patterns and knowledge evolution
- **Collaboration**: Share conversation exports with team members
- **Compliance**: Maintain complete audit trail of AI interactions

### Exception Handling & Plan Enforcement
```java
// Custom exceptions for plan limits
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PlanLimitExceededException extends RuntimeException {
    private final String limitType;
    private final String planRequired;
    
    public PlanLimitExceededException(String message, String limitType, String planRequired) {
        super(message);
        this.limitType = limitType;
        this.planRequired = planRequired;
    }
    
    // Response format for mobile app
    public ErrorResponse toErrorResponse() {
        return ErrorResponse.builder()
            .error("PLAN_LIMIT_EXCEEDED")
            .message(getMessage())
            .limitType(limitType)
            .planRequired(planRequired)
            .upgradeUrl("/api/subscription/upgrade")
            .build();
    }
}

@ResponseStatus(HttpStatus.FORBIDDEN)
public class FeatureNotAvailableException extends RuntimeException {
    private final String feature;
    private final String planRequired;
    
    public FeatureNotAvailableException(String message, String feature, String planRequired) {
        super(message);
        this.feature = feature;
        this.planRequired = planRequired;
    }
}

// Global exception handler
@ControllerAdvice
public class SubscriptionExceptionHandler {
    
    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handlePlanLimitExceeded(PlanLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
            .body(ex.toErrorResponse());
    }
    
    @ExceptionHandler(FeatureNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleFeatureNotAvailable(FeatureNotAvailableException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ErrorResponse.builder()
                .error("FEATURE_NOT_AVAILABLE")
                .message(ex.getMessage())
                .feature(ex.getFeature())
                .planRequired(ex.getPlanRequired())
                .upgradeUrl("/api/subscription/upgrade")
                .build());
    }
}
```

### Middleware & Interceptors
```java
// Plan limit checking interceptor
@Component
public class PlanLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private UsageLimitService usageLimitService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) 
            throws Exception {
        
        // Skip for auth endpoints
        if (request.getRequestURI().startsWith("/api/auth")) {
            return true;
        }
        
        Long userId = getCurrentUserId(request);
        if (userId == null) return true;
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // Check specific endpoints for plan limits
        if ("POST".equals(method) && uri.matches("/api/projects$")) {
            if (!usageLimitService.canCreateProject(userId)) {
                throw new PlanLimitExceededException(
                    "Project limit exceeded. Upgrade to Pro for more projects.", 
                    "PROJECTS", 
                    "PRO"
                );
            }
        }
        
        if ("POST".equals(method) && uri.matches("/api/projects/\\d+/knowledge-sources$")) {
            Long projectId = extractProjectId(uri);
            if (!usageLimitService.canAddKnowledgeSource(userId, projectId)) {
                throw new PlanLimitExceededException(
                    "Knowledge source limit exceeded for this project.", 
                    "KNOWLEDGE_SOURCES", 
                    "PRO"
                );
            }
        }
        
        if ("POST".equals(method) && uri.matches("/api/chat/sessions/\\d+/messages$")) {
            if (!usageLimitService.canSendMessage(userId)) {
                throw new PlanLimitExceededException(
                    "Monthly message limit exceeded. Upgrade to Pro for unlimited messages.", 
                    "MESSAGES", 
                    "PRO"
                );
            }
        }
        
        return true;
    }
}
```

### Data Transfer Objects (DTOs)
```java
// Subscription DTOs
@Data
@Builder
public class SubscriptionPlanDTO {
    private String planName;
    private String displayName;
    private String description;
    private BigDecimal priceMonthly;
    private BigDecimal priceYearly;
    private PlanLimitsDTO limits;
    private PlanFeaturesDTO features;
    private boolean isCurrentPlan;
    private boolean isRecommended;
}

@Data
@Builder
public class PlanLimitsDTO {
    private int maxProjects;
    private int maxKnowledgeSourcesPerProject;
    private int maxFileSizeMB;
    private int maxMonthlyMessages;
    private BigDecimal maxStorageGB;
}

@Data
@Builder  
public class PlanFeaturesDTO {
    private boolean canUseWebUrls;
    private boolean canExportConversations;
    private boolean canUseAdvancedAI;
    private boolean canUseSemanticSearch;
    private boolean hasPrioritySupport;
}

@Data
@Builder
public class UsageDetailsDTO {
    private String currentMonth;
    private UsageCountsDTO usage;
    private PlanLimitsDTO limits;
    private UsagePercentageDTO percentageUsed;
    private List<UsageWarningDTO> warnings;
}

@Data
@Builder
public class UsageCountsDTO {
    private int projectsCreated;
    private int knowledgeSourcesAdded;
    private int messagesSent;
    private BigDecimal storageUsedMB;
    private int aiTokensUsed;
}

@Data
@Builder
public class UsagePercentageDTO {
    private double projects;
    private double messages;
    private double storage;
}

@Data
@Builder
public class UsageWarningDTO {
    private String type; // "APPROACHING_LIMIT", "LIMIT_EXCEEDED"
    private String resource; // "PROJECTS", "MESSAGES", "STORAGE"
    private String message;
    private double percentageUsed;
    private boolean requiresUpgrade;
}

@Data
@Builder
public class UpgradeRequestDTO {
    private String targetPlan;
    private String billingCycle; // "MONTHLY", "YEARLY"
    private String paymentMethodId; // Stripe payment method ID
    private boolean autoRenew;
}

@Data
@Builder
public class UpgradeResponseDTO {
    private boolean success;
    private String message;
    private String subscriptionId;
    private String invoiceUrl;
    private UserSubscriptionDetailsDTO subscription;
}
```

## 🔄 Processing Flows (Enhanced with Plan Management)

### 1. User Registration Flow
```mermaid
graph TD
    A[User opens app] --> B{Registration method?}
    B -->|Email| C[Enter email]
    B -->|Google| D[Google OAuth]
    
    C --> E[Send OTP]
    E --> F[Enter OTP + Full name + Preferred name]
    F --> G[Verify OTP]
    G --> H[Create user account with FREE plan]
    
    D --> I[Verify Google token]
    I --> J[Extract user info]
    J --> K[Create/update user account with FREE plan]
    
    H --> L[Generate JWT]
    K --> L
    L --> M[Store in Redis]
    M --> N[Return user data + token + plan limits]
```

### 2. Document Processing Flow with S3 & Queues
```mermaid
graph TD
    A[User uploads document] --> B[Spring Boot: Validate plan limits]
    B --> C[Upload file to S3]
    C --> D[Save knowledge_source record with S3 path]
    D --> E[Publish message to RabbitMQ: document.processing.queue]
    
    E --> F[Python Service: Receive processing request]
    F --> G[Download file from S3]
    G --> H[Extract content using appropriate library]
    H --> I[Clean and preprocess text]
    I --> J[Chunk content with metadata]
    J --> K[Publish success message to: document.processing.success]
    
    K --> L[Spring Boot: Receive processed chunks]
    L --> M[Generate embeddings with OpenAI]
    M --> N[Store vectors in Qdrant knowledge collection]
    N --> O[Update knowledge_source status to COMPLETED]
    O --> P[Send WebSocket notification to mobile app]
    
    style F fill:#f9f,stroke:#333,stroke-width:2px
    style G fill:#f9f,stroke:#333,stroke-width:2px
    style H fill:#f9f,stroke:#333,stroke-width:2px
    style I fill:#f9f,stroke:#333,stroke-width:2px
    style J fill:#f9f,stroke:#333,stroke-width:2px
```

### 3. Web URL Processing Flow with Crawl4AI
```mermaid
graph TD
    A[User submits web URL] --> B[Spring Boot: Validate Pro plan feature]
    B --> C[Save knowledge_source record with URL]
    C --> D[Publish message to RabbitMQ: url.processing.queue]
    
    D --> E[Python Service: Receive URL processing request]
    E --> F[Initialize Crawl4AI AsyncWebCrawler]
    F --> G[Crawl URL with content extraction strategy]
    G --> H[Clean and structure web content]
    H --> I[Extract metadata and assets]
    I --> J[Chunk content with web-specific metadata]
    J --> K[Publish success message to: url.processing.success]
    
    K --> L[Spring Boot: Receive processed web content]
    L --> M[Generate embeddings with OpenAI]
    M --> N[Store vectors in Qdrant knowledge collection]
    N --> O[Update knowledge_source status to COMPLETED]
    O --> P[Send WebSocket notification to mobile app]
    
    style E fill:#9ff,stroke:#333,stroke-width:2px
    style F fill:#9ff,stroke:#333,stroke-width:2px
    style G fill:#9ff,stroke:#333,stroke-width:2px
    style H fill:#9ff,stroke:#333,stroke-width:2px
    style I fill:#9ff,stroke:#333,stroke-width:2px
    style J fill:#9ff,stroke:#333,stroke-width:2px
```

### 4. Enhanced Chat Session Flow (Triple-Layer Intelligence)
```mermaid
graph TD
    A[User selects project] --> B[Create/select chat session]
    B --> C[Load project context: instructions + knowledge]
    C --> D[Load complete conversation history if resuming session]
    D --> E[User sends message]
    E --> F[Spring Boot: Check message limits]
    F --> G[Triple-Layer AI Processing]
    
    G --> H[Layer 1: RAG - Query Qdrant Knowledge Base]
    H --> I[Retrieve: Project documents + web content]
    
    G --> J[Layer 2: Semantic Memory - Query Qdrant Chat Memory (Pro only)]
    J --> K[Retrieve: Semantically similar past conversations]
    
    G --> L[Layer 3: Sequential Memory - Query Recent History]
    L --> M[Retrieve: Last 5-10 messages for immediate context]
    
    I --> N[Combine Enhanced Context]
    K --> N
    M --> N
    
    N --> O[Build super-enhanced prompt with:
              - Project instructions
              - User's preferred name
              - Retrieved knowledge documents
              - Semantically relevant past conversations (Pro)
              - Recent conversation flow
              - Current user query]
    
    O --> P[Send to OpenAI LLM]
    P --> Q[Stream intelligent response to mobile app]
    Q --> R[PERMANENTLY save both user message and AI response to PostgreSQL]
    R --> S[Auto-vectorize conversation for future semantic search (Pro only)]
    S --> T[Store chat vectors in Qdrant chat_memory collection]
    T --> U[Update session metadata and usage tracking]
    U --> V[Conversation stored forever with dual access:
              - Traditional browsing (PostgreSQL)
              - Semantic search (Qdrant vectors for Pro users)]
```

### 5. Error Handling Flow
```mermaid
graph TD
    A[Document/URL Processing Error] --> B[Python Service: Detect error]
    B --> C[Classify error type]
    C --> D[Publish error message to: document.processing.error]
    
    D --> E[Spring Boot: Receive error notification]
    E --> F[Update knowledge_source status to FAILED]
    F --> G[Log error details]
    G --> H[Send error notification to mobile app]
    H --> I[Display user-friendly error message]
    I --> J[Suggest remediation action]
```

## 📱 Mobile App Integration Points

### Authentication
```typescript
// Mobile app authentication service
interface AuthService {
  signupWithEmail(email: string): Promise<{success: boolean, message: string}>;
  verifyOTP(email: string, otp: string, fullName: string, preferredName: string): Promise<AuthResponse>;
  signupWithGoogle(): Promise<AuthResponse>;
  refreshToken(): Promise<AuthResponse>;
  logout(): Promise<void>;
}
```

### Subscription Management (NEW)
```typescript
interface SubscriptionService {
  getAvailablePlans(): Promise<SubscriptionPlan[]>;
  getCurrentSubscription(): Promise<UserSubscriptionDetails>;
  getCurrentUsage(): Promise<UsageDetails>;
  getCurrentLimits(): Promise<PlanLimits>;
  upgradeToPro(paymentToken: string): Promise<UpgradeResponse>;
  downgradeToFree(): Promise<void>;
  cancelSubscription(): Promise<void>;
  
  // Plan limit checking methods
  canCreateProject(): Promise<boolean>;
  canAddKnowledgeSource(projectId: string): Promise<boolean>;
  canSendMessage(): Promise<boolean>;
  canUploadFile(fileSizeBytes: number): Promise<boolean>;
  canUseWebUrls(): Promise<boolean>;
  canExportConversations(): Promise<boolean>;
  canUseSemanticSearch(): Promise<boolean>;
}

interface SubscriptionPlan {
  planName: string;
  displayName: string;
  description: string;
  priceMonthly: number;
  priceYearly: number;
  limits: {
    maxProjects: number;
    maxKnowledgeSourcesPerProject: number;
    maxFileSizeMB: number;
    maxMonthlyMessages: number;
    maxStorageGB: number;
  };
  features: {
    canUseWebUrls: boolean;
    canExportConversations: boolean;
    canUseAdvancedAI: boolean;
    canUseSemanticSearch: boolean;
    hasPrioritySupport: boolean;
  };
}

interface UserSubscriptionDetails {
  currentPlan: string;
  planStartedAt: Date;
  planExpiresAt?: Date;
  subscriptionStatus: 'ACTIVE' | 'CANCELLED' | 'EXPIRED' | 'PAST_DUE';
  autoRenew: boolean;
  nextBillingDate?: Date;
}

interface UsageDetails {
  currentMonth: string;
  projectsCreated: number;
  knowledgeSourcesAdded: number;
  messagesSent: number;
  storageUsedMB: number;
  limits: {
    maxProjects: number;
    maxKnowledgeSourcesPerProject: number;
    maxMonthlyMessages: number;
    maxStorageGB: number;
  };
  percentageUsed: {
    projects: number;
    messages: number;
    storage: number;
  };
}

interface PlanLimits {
  planName: string;
  maxProjects: number;
  currentProjects: number;
  maxKnowledgeSourcesPerProject: number;
  maxMonthlyMessages: number;
  currentMonthlyMessages: number;
  maxFileSizeMB: number;
  maxStorageGB: number;
  currentStorageGB: number;
  features: PlanFeatures;
}

interface PlanFeatures {
  canUseWebUrls: boolean;
  canExportConversations: boolean;
  canUseAdvancedAI: boolean;
  canUseSemanticSearch: boolean;
  hasPrioritySupport: boolean;
}
```

### Enhanced Project Management (With Limits)
```typescript
interface ProjectService {
  createProject(data: CreateProjectData): Promise<ProjectResponse>;
  getProjects(): Promise<ProjectSummary[]>;
  addKnowledgeSource(projectId: string, source: KnowledgeSourceData): Promise<KnowledgeSourceResponse>;
  getProcessingStatus(jobId: string): Promise<ProcessingStatus>;
  
  // Limit-aware methods
  checkProjectCreationLimit(): Promise<{canCreate: boolean, reason?: string}>;
  checkKnowledgeSourceLimit(projectId: string): Promise<{canAdd: boolean, reason?: string}>;
  checkFileSizeLimit(fileSizeBytes: number): Promise<{canUpload: boolean, reason?: string}>;
  checkWebUrlFeature(): Promise<{canUse: boolean, reason?: string}>;
}

interface ProjectResponse {
  project: Project;
  limits?: {
    knowledgeSourcesUsed: number;
    knowledgeSourcesLimit: number;
    canAddMore: boolean;
    requiresUpgrade: boolean;
  };
}
```

### Enhanced Chat Interface (Plan-Aware Features)
```typescript
interface ChatService {
  // Session Management
  createSession(projectId: string, name?: string): Promise<ChatSession>;
  getSessions(projectId: string, includeArchived?: boolean): Promise<ChatSession[]>;
  archiveSession(sessionId: string): Promise<void>;
  renameSession(sessionId: string, newName: string): Promise<void>;
  
  // Enhanced Messaging with AI (Plan-aware)
  sendMessage(sessionId: string, message: string): Promise<MessageResponse>;
  getMessages(sessionId: string, page: number): Promise<ChatMessage[]>;
  getAllMessages(sessionId: string): Promise<ChatMessage[]>;
  
  // Premium Features (Pro Plan Only)
  searchSimilarConversations(projectId: string, query: string): Promise<SemanticSearchResult[]>;
  findRelatedDiscussions(projectId: string, topic: string): Promise<ChatMessage[]>;
  getConversationInsights(projectId: string, timeRange?: DateRange): Promise<ConversationInsights>;
  exportConversation(sessionId: string, format: 'json' | 'markdown'): Promise<string>;
  getConversationThreads(projectId: string): Promise<ConversationThread[]>;
  
  // Plan validation methods
  checkMessageLimit(): Promise<{canSend: boolean, remaining: number, requiresUpgrade: boolean}>;
  checkSemanticSearchAccess(): Promise<{hasAccess: boolean, requiresUpgrade: boolean}>;
  checkExportAccess(): Promise<{hasAccess: boolean, requiresUpgrade: boolean}>;
}

interface MessageResponse {
  content: AsyncIterator<string>; // Streaming response
  metadata: {
    aiLayers: {
      ragSources: number;
      semanticMatches: number; // 0 for free users
      recentContext: number;
    };
    tokensUsed: number;
    remainingMessages?: number; // For free plan users
    requiresUpgrade?: boolean;
  };
}
```

### Plan Upgrade Flow Management
```typescript
interface UpgradeFlowService {
  // Upgrade prompts and modals
  showProjectLimitModal(): Promise<{userAction: 'upgrade' | 'cancel'}>;
  showKnowledgeSourceLimitModal(): Promise<{userAction: 'upgrade' | 'cancel'}>;
  showMessageLimitModal(): Promise<{userAction: 'upgrade' | 'cancel'}>;
  showFeatureUnavailableModal(feature: string): Promise<{userAction: 'upgrade' | 'cancel'}>;
  
  // Upgrade process
  initiateUpgrade(plan: string): Promise<{paymentUrl: string}>;
  handleUpgradeSuccess(transactionId: string): Promise<void>;
  handleUpgradeFailure(error: string): Promise<void>;
  
  // Usage warnings
  showUsageWarning(usageType: 'projects' | 'messages' | 'storage', percentageUsed: number): void;
  showApproachingLimitNotification(limit: string, remaining: number): void;
}
```

### Chat Interface (Advanced Conversation Management + Semantic Search)
```typescript
interface ChatService {
  // Session Management
  createSession(projectId: string, name?: string): Promise<ChatSession>;
  getSessions(projectId: string, includeArchived?: boolean): Promise<ChatSession[]>;
  archiveSession(sessionId: string): Promise<void>;
  renameSession(sessionId: string, newName: string): Promise<void>;
  
  // Enhanced Messaging with AI
  sendMessage(sessionId: string, message: string): Promise<AsyncIterator<string>>; // Triple-layer AI streaming
  getMessages(sessionId: string, page: number): Promise<ChatMessage[]>;
  getAllMessages(sessionId: string): Promise<ChatMessage[]>; // Complete conversation history
  
  // Semantic Search Capabilities (NEW)
  searchSimilarConversations(projectId: string, query: string): Promise<SemanticSearchResult[]>;
  findRelatedDiscussions(projectId: string, topic: string): Promise<ChatMessage[]>;
  getConversationInsights(projectId: string, timeRange?: DateRange): Promise<ConversationInsights>;
  
  // Advanced History Management
  searchMessages(projectId: string, query: string): Promise<ChatMessage[]>; // Search across all conversations
  exportConversation(sessionId: string, format: 'json' | 'markdown'): Promise<string>;
  getConversationThreads(projectId: string): Promise<ConversationThread[]>; // AI-grouped related discussions
  
  // Analytics & Intelligence
  getConversationStats(projectId: string): Promise<{
    totalSessions: number;
    totalMessages: number;
    activeSessionsCount: number;
    oldestConversation: Date;
    mostDiscussedTopics: string[];
    knowledgeGaps: string[]; // Topics with many questions but few answers
  }>;
  
  // Memory & Context Insights
  getChatMemoryHealth(projectId: string): Promise<{
    vectorizedMessages: number;
    semanticCoverage: number; // % of conversations that can be found semantically
    memoryEfficiency: number; // How well the system remembers context
  }>;
}

interface SemanticSearchResult {
  messageId: string;
  sessionId: string;
  sessionName: string;
  content: string;
  similarity: number;
  timestamp: Date;
  conversationContext: string; // Surrounding messages for context
}

interface ConversationThread {
  threadId: string;
  topic: string;
  relatedSessions: string[];
  messageCount: number;
  spanDays: number;
  lastActivity: Date;
  keyInsights: string[];
}

interface ConversationInsights {
  topTopics: Array<{topic: string, frequency: number}>;
  questionPatterns: Array<{pattern: string, count: number}>;
  knowledgeEvolution: Array<{date: Date, newConcepts: string[]}>;
  conversationFlow: {
    averageSessionLength: number;
    peakActivityHours: string[];
    questionToAnswerRatio: number;
  };
}

interface ChatSession {
  id: string;
  projectId: string;
  name: string;
  messageCount: number;
  lastMessageAt: Date;
  createdAt: Date;
  isActive: boolean; // For UI organization only
  preview: string; // Last few messages for preview
  topics: string[]; // AI-extracted topics from conversation
  semanticSummary?: string; // AI-generated summary for better context
}

interface ChatMessage {
  id: string;
  sessionId: string;
  type: 'USER' | 'ASSISTANT' | 'SYSTEM';
  content: string;
  timestamp: Date;
  vectorId?: string; // Reference to semantic search vector
  conversationGroupId?: string; // For Q&A pairing
  metadata?: {
    tokensUsed?: number;
    sourcesReferenced?: string[]; // Knowledge sources used
    similarConversations?: string[]; // Past conversations referenced
    processingTime?: number;
    aiLayers?: {
      ragSources: number;
      semanticMatches: number;
      recentContext: number;
    };
  };
}
```

## ⚙️ Configuration Files

### Spring Boot Application.yml
```yaml
spring:
  application:
    name: mobile-rag-backend
    
  profiles:
    active: ${ENVIRONMENT:development}
    
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:mobile_rag}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    password: ${REDIS_PASSWORD:}
    timeout: 2000ms
    jedis:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4
        temperature: 0.7
      embedding:
        model: text-embedding-ada-002
        
    vectorstore:
      qdrant:
        # Main knowledge base collection
        host: ${QDRANT_HOST:localhost}
        port: ${QDRANT_PORT:6334}
        api-key: ${QDRANT_API_KEY:}
        collection-name: project_knowledge
        initialize-schema: true
        
      # Custom configuration for dual collections
      knowledge:
        host: ${QDRANT_HOST:localhost}
        port: ${QDRANT_PORT:6334}
        api-key: ${QDRANT_API_KEY:}
        collection-name: project_knowledge
        
      chat-memory:
        host: ${QDRANT_HOST:localhost}
        port: ${QDRANT_PORT:6334}
        api-key: ${QDRANT_API_KEY:}
        collection-name: chat_memory

server:
  port: ${SERVER_PORT:8080}
  
# Custom application properties
app:
  jwt:
    secret: ${JWT_SECRET:your-secret-key-change-in-production}
    expiration: 86400000 # 24 hours in milliseconds
    
  otp:
    expiration: 300 # 5 minutes in seconds
    length: 6
    
  file-upload:
    max-size: 50MB # Will be limited per plan
    allowed-types: pdf,doc,docx,txt,md
    storage-path: ${STORAGE_PATH:/app/storage}
    
  rate-limit:
    auth:
      requests: 5
      window: 3600 # 1 hour
    api:
      requests: 500 # Reduced for side project scale
      window: 3600 # 1 hour

  # Simple Subscription Management (Individual Users Only)
  subscription:
    stripe:
      secret-key: ${STRIPE_SECRET_KEY}
      webhook-secret: ${STRIPE_WEBHOOK_SECRET}
      success-url: ${FRONTEND_URL}/subscription/success
      cancel-url: ${FRONTEND_URL}/subscription/cancel
    
    plans:
      free:
        max-projects: 3
        max-knowledge-sources: 5
        max-file-size-mb: 10
        max-monthly-messages: 100
        max-storage-gb: 0.5
        features:
          web-urls: false
          export-conversations: false
          advanced-ai: false
          semantic-search: false
          priority-support: false
      
      pro:
        max-projects: 25
        max-knowledge-sources: 100
        max-file-size-mb: 50
        max-monthly-messages: 1000
        max-storage-gb: 5.0
        features:
          web-urls: true
          export-conversations: true
          advanced-ai: true
          semantic-search: true
          priority-support: true

  # Simplified AI & Processing Limits
  ai:
    openai:
      rate-limits:
        free-tier: 5 # requests per minute (conservative for side project)
        pro-tier: 20 # requests per minute
    
    processing:
      max-concurrent-jobs: 3 # Reduced for simpler infrastructure
      timeout-minutes: 15

# Logging
logging:
  level:
    com.yourcompany.mobilerag: DEBUG
    org.springframework.security: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### Python Service Configuration
```python
# config.py
import os
from pydantic import BaseSettings

class Settings(BaseSettings):
    # API Configuration
    API_HOST: str = "0.0.0.0"
    API_PORT: int = 8081
    
    # RabbitMQ
    RABBITMQ_URL: str = "amqp://guest:guest@localhost:5672"
    
    # Crawl4AI Settings
    CRAWL4AI_MAX_CONCURRENT: int = 5
    CRAWL4AI_TIMEOUT: int = 30
    CRAWL4AI_USER_AGENT: str = "MobileRAG-Crawler/1.0"
    
    # File Processing
    MAX_FILE_SIZE: int = 50 * 1024 * 1024  # 50MB
    CHUNK_SIZE: int = 1000
    CHUNK_OVERLAP: int = 200
    
    # OpenAI
    OPENAI_API_KEY: str
    
    # Logging
    LOG_LEVEL: str = "INFO"
    
    class Config:
        env_file = ".env"

settings = Settings()
```

## 🐳 Docker Compose Setup

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: mobile-rag-postgres
    environment:
      POSTGRES_DB: mobile_rag
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password123
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 30s
      timeout: 10s
      retries: 3

  redis:
    image: redis:7-alpine
    container_name: mobile-rag-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  qdrant:
    image: qdrant/qdrant:latest
    container_name: mobile-rag-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_data:/qdrant/storage
    environment:
      QDRANT__SERVICE__HTTP_PORT: 6333
      QDRANT__SERVICE__GRPC_PORT: 6334
    # Note: Will auto-create both collections:
    # - project_knowledge (RAG documents)  
    # - chat_memory (semantic conversation memory)

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: mobile-rag-rabbitmq
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 30s
      timeout: 10s
      retries: 3

  spring-backend:
    build: 
      context: ./spring-backend
      dockerfile: Dockerfile
    container_name: mobile-rag-backend
    ports:
      - "8080:8080"
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: mobile_rag
      DB_USERNAME: postgres
      DB_PASSWORD: password123
      REDIS_HOST: redis
      REDIS_PORT: 6379
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      QDRANT_HOST: qdrant
      QDRANT_PORT: 6334
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      qdrant:
        condition: service_started
    restart: unless-stopped

  python-processor:
    build:
      context: ./python-processor
      dockerfile: Dockerfile
    container_name: mobile-rag-processor
    ports:
      - "8081:8081"
    environment:
      RABBITMQ_URL: amqp://guest:guest@rabbitmq:5672
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      LOG_LEVEL: INFO
    depends_on:
      rabbitmq:
        condition: service_healthy
    restart: unless-stopped
    volumes:
      - document_storage:/app/storage

volumes:
  postgres_data:
  redis_data:
  qdrant_data:
  rabbitmq_data:
  document_storage:

networks:
  default:
    name: mobile-rag-network
```

## 🔒 Security Implementation

### JWT Authentication
```java
@Component
public class JwtTokenProvider {
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public String generateToken(User user) {
        Date expiryDate = new Date(System.currentTimeMillis() + jwtExpirationInMs);
        
        String token = Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getPreferredName())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .compact();
        
        // Store in Redis for validation
        String tokenKey = "jwt:user:" + user.getId() + ":" + DigestUtils.sha256Hex(token);
        UserSession session = new UserSession(user.getId(), token, expiryDate);
        redisTemplate.opsForValue().set(tokenKey, session, Duration.ofMillis(jwtExpirationInMs));
        
        return token;
    }
    
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody();
            Long userId = Long.parseLong(claims.getSubject());
            String tokenKey = "jwt:user:" + userId + ":" + DigestUtils.sha256Hex(token);
            return redisTemplate.hasKey(tokenKey);
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Rate Limiting
```java
@Component
public class RateLimitingFilter implements Filter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String clientIp = getClientIpAddress(httpRequest);
        String path = httpRequest.getRequestURI();
        
        if (path.startsWith("/api/auth/")) {
            if (!checkAuthRateLimit(clientIp)) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean checkAuthRateLimit(String clientIp) {
        String key = "rate_limit:auth:" + clientIp;
        String count = (String) redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            redisTemplate.opsForValue().set(key, "1", Duration.ofHours(1));
            return true;
        }
        
        int currentCount = Integer.parseInt(count);
        if (currentCount >= 5) { // Max 5 auth attempts per hour
            return false;
        }
        
        redisTemplate.opsForValue().increment(key);
        return true;
    }
}
```

## 🧠 Spring AI Semantic Memory Integration

### **How VectorStoreChatMemoryAdvisor Works**

Spring AI's `VectorStoreChatMemoryAdvisor` provides intelligent conversation memory through semantic vector search. Unlike traditional chat memory that only remembers recent messages chronologically, this system finds the most *relevant* past conversations based on meaning and context.

```java
// Configuration Example
@Bean 
public VectorStore chatMemoryVectorStore() {
    return QdrantVectorStore.builder()
        .client(qdrantClient)
        .collectionName("chat_memory")
        .embeddingModel(embeddingModel)
        .initializeSchema(true)
        .build();
}

@Bean
public ChatClient enhancedChatClient() {
    return ChatClient.builder(chatModel)
        .defaultAdvisors(
            // RAG for knowledge documents
            RetrievalAugmentationAdvisor.builder()
                .documentRetriever(knowledgeRetriever)
                .build(),
                
            // Semantic chat memory - THE GAME CHANGER
            VectorStoreChatMemoryAdvisor.builder()
                .vectorStore(chatMemoryVectorStore)
                .chatHistoryWindowSize(20) // Top 20 most relevant conversations
                .similarityThreshold(0.7)  // Minimum relevance score
                .promptTemplate(customMemoryTemplate) // Custom prompt integration
                .build()
        )
        .build();
}
```

### **Semantic vs Traditional Memory Comparison**

```java
// Traditional Sequential Memory (Last N messages)
User: "What marketing strategies did we discuss?"
Memory: [Last 10 messages in chronological order]
Problem: Marketing discussion was 3 weeks ago - not in recent messages!

// Semantic Vector Memory (Most relevant conversations)
User: "What marketing strategies did we discuss?"  
Memory: [All marketing-related conversations ranked by relevance]
Result: Finds marketing discussions from weeks/months ago automatically!
```

### **Technical Integration Points**

1. **Automatic Vectorization**: Every chat message is automatically converted to embeddings and stored in Qdrant
2. **Project Isolation**: Vector search is filtered by project ID to maintain data separation
3. **Conversation Grouping**: Q&A pairs are grouped for better context retrieval
4. **Dynamic Filtering**: Search can be refined by session, time range, or topic
5. **Performance Optimization**: Vector search happens in milliseconds, not blocking the chat flow

### **Memory Template Customization**

```java
// Custom prompt template for better memory integration
@Bean
public PromptTemplate semanticMemoryTemplate() {
    return new PromptTemplate("""
        You are an AI assistant for {user_preferred_name} working on the "{project_name}" project.
        
        Project Instructions: {instructions}
        
        Knowledge Context: {knowledge_context}
        
        Relevant Past Conversations: 
        {long_term_memory}
        
        Recent Conversation Flow:
        {memory}
        
        Current User Question: {input}
        
        Provide a helpful response that builds upon the knowledge context and past conversations.
        Reference specific insights from previous discussions when relevant.
        """);
}
```

### **Benefits Over Traditional RAG Systems**

| Traditional RAG | Enhanced RAG + Semantic Memory |
|---|---|
| Only searches documents | Searches documents + all past conversations |
| No conversation continuity | Perfect conversation continuity across sessions |
| Keyword-based search | Semantic understanding of intent |
| Forgets previous insights | Builds upon all previous insights |
| Static knowledge | Dynamic, evolving knowledge from conversations |

## 💰 Subscription Business Model

### **Plan Comparison Table (Individual Users)**

| Feature | Free Plan | Pro Plan |
|---|---|---|
| **Price** | $0/month | $9.99/month or $99.99/year |
| **Projects** | 3 projects | 25 projects |
| **Knowledge Sources per Project** | 5 sources | 100 sources |
| **File Upload Size** | 10MB max | 50MB max |
| **Monthly Messages** | 100 messages | 1,000 messages |
| **Storage** | 500MB | 5GB |
| **Web URL Processing** | ❌ | ✅ |
| **Conversation Export** | ❌ | ✅ |
| **Semantic Chat Search** | ❌ | ✅ Advanced AI Memory |
| **Advanced AI Features** | Basic | Full Triple-Layer Intelligence |
| **Priority Support** | ❌ | ✅ |

### **Simple Freemium Strategy (Individual Focus)**

**For Individual Users:**
- **Easy Start**: Try core RAG functionality with generous free limits
- **Personal Productivity**: Organize knowledge for studies, work, or hobbies
- **Natural Growth**: Upgrade when you need more projects or advanced features
- **Affordable Premium**: Pro plan priced for individual users, not businesses

**For Side Project Success:**
- **Low Maintenance**: Simple two-tier system without enterprise complexity
- **Clear Value Proposition**: Pay for AI intelligence and unlimited usage
- **Sustainable Costs**: Pricing covers infrastructure and development time
- **User Focused**: Features that matter to individual productivity

**Upgrade Triggers for Individuals:**
```typescript
// When personal users hit meaningful limits
- Creating 4th project → "Upgrade for 25 projects"
- Adding 6th knowledge source → "Upgrade for 100 sources per project"  
- Sending 101st message → "Upgrade for 1,000 monthly conversations"
- Uploading larger files → "Upgrade for 50MB file support"
- Processing websites → "Upgrade to save articles and web content"
- Finding old conversations → "Upgrade for AI-powered conversation search"
```

### **Realistic Revenue Goals (Side Project)**

**Conservative Targets (Month 12):**
```yaml
User Acquisition:
  - Total Users: 2,000 (realistic for side project)
  - Free Users: 1,700 (85%)
  - Pro Users: 300 (15% conversion)

Monthly Revenue:
  - Pro Subscriptions: 300 × $9.99 = $2,997/month
  - Annual Subscriptions (30% of Pro): 90 × $99.99/year = $8,999/year
  - Combined MRR: ~$3,750/month
  - ARR Projection: ~$45,000/year

Cost Structure:
  - OpenAI API Costs: ~$800/month
  - Infrastructure: ~$300/month  
  - Total COGS: ~$1,100/month
  - Net Revenue: ~$2,650/month
  - Personal Income: ~$31,800/year (great side project income!)
```

**Growth Targets (Side Project Scale):**
- **Year 1**: 300 Pro users, $45K ARR
- **Year 2**: 800 Pro users, $95K ARR
- **Year 3**: 1,500 Pro users, $150K ARR (solid side income)

### **Simplified Feature Roadmap**

**Focus Areas:**
- **Individual Productivity**: Features that help personal knowledge management
- **Ease of Use**: Simple, intuitive interface for individual users
- **Reliable Core**: Solid RAG + semantic memory without enterprise complexity
- **Sustainable Growth**: Features that matter to individual users, not businesses

**No Enterprise Features:**
- No team collaboration or multi-user workspaces
- No SSO or enterprise security requirements
- No custom model training or white-label solutions
- No complex admin controls or user management
- No enterprise integrations or API access

## 📊 Monitoring & Analytics

### Health Check Endpoints
```java
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    @GetMapping
    public ResponseEntity<HealthStatus> getHealth() {
        HealthStatus status = HealthStatus.builder()
                .status("UP")
                .timestamp(Instant.now())
                .services(checkServices())
                .build();
        return ResponseEntity.ok(status);
    }
    
    private Map<String, String> checkServices() {
        Map<String, String> services = new HashMap<>();
        services.put("database", checkDatabase());
        services.put("redis", checkRedis());
        services.put("qdrant", checkQdrant());
        services.put("rabbitmq", checkRabbitMQ());
        services.put("openai", checkOpenAI());
        return services;
    }
}
```

### Performance Metrics
```yaml
Metrics to Track:
  - User registration/login rates
  - Project creation frequency
  - Knowledge source processing times
  - Chat session duration and message count
  - Token usage and costs
  - System resource utilization
  - Error rates and response times
```

## 🚀 Deployment Strategy

### Environment Configuration
```yaml
Development:
  - Local Docker Compose
  - Hot reload enabled
  - Debug logging
  - Test data seeding

Staging:
  - Kubernetes cluster
  - Production-like data
  - Performance testing
  - Security scanning

Production:
  - Kubernetes with auto-scaling
  - Multi-zone deployment
  - Monitoring and alerting
  - Backup and disaster recovery
```

## 📈 Scalability Considerations

### Horizontal Scaling Points
1. **Spring Boot Backend**: Multiple instances behind load balancer
2. **Python Processors**: Scale based on queue length
3. **Database**: Read replicas for read-heavy operations
4. **Redis**: Redis Cluster for high availability
5. **Qdrant**: Horizontal scaling with sharding

### Performance Optimization
- Connection pooling for all services
- Async processing for heavy operations
- Caching strategies at multiple levels
- Database query optimization
- CDN for static assets

## 💰 Cost Optimization

### OpenAI Usage Optimization
- Implement embedding caching to avoid re-processing
- Optimize chunk sizes for embedding efficiency
- Use streaming for better user experience
- Monitor and alert on token usage

### Infrastructure Costs
- Auto-scaling based on demand
- Spot instances for non-critical workloads
- Resource limits to prevent runaway costs
- Regular cost analysis and optimization

## 🔄 Future Enhancements

### Phase 1 Features (Post-MVP) - Individual User Focus
- Voice message support with transcription
- Enhanced PDF/document processing
- **Usage analytics dashboard** for Pro users
- **Conversation search and organization** improvements
- **Mobile app optimizations** and offline reading
- **Knowledge source organization** (folders, tags, favorites)
- **Export and backup features** for Pro users
- **Dark mode and customization** options
- **Conversation templates** for common use cases
- **Smart notifications** for processing updates

### Phase 2 Enhancements - Individual Productivity
- **Cross-platform sync** (iOS, Android, Web)
- **Improved semantic search** with filters and date ranges
- **AI-powered conversation summaries**
- **Knowledge source recommendations** based on conversations
- **Advanced file format support** (more document types)
- **Integration with popular apps** (Notion, Obsidian, etc.)
- **Conversation analytics** (topics, patterns, insights)
- **Improved mobile UX** with better navigation
- **Offline conversation access** and sync
- **Advanced search filters** and sorting

### Maintenance & Long-term Vision - Sustainable Side Project
- **Focus on core features** that individual users love
- **Stable, reliable service** with minimal maintenance overhead
- **Community-driven feature requests** from engaged users
- **Gradual AI improvements** as models get better and cheaper
- **Simple monetization** without complex enterprise features
- **Open-source components** where possible to reduce maintenance
- **Automated operations** to minimize manual work
- **Cost optimization** to maintain profitability at scale

**🎯 Side Project Success Criteria:**
- ✅ **Sustainable Income**: $30-80K/year personal income
- ✅ **Low Maintenance**: <10 hours/week ongoing work
- ✅ **Happy Users**: Focused on individual productivity and learning
- ✅ **Technical Excellence**: Reliable, fast, well-designed system
- ✅ **Profitable Growth**: Sustainable unit economics at small scale

---

**🔑 Key Architecture Advantage**: The simplified, individual-focused approach creates a **sustainable side project** with clear value proposition, manageable complexity, and realistic revenue goals. No enterprise bloat, just solid RAG + semantic memory for personal productivity.

**🚀 Side Project Differentiator**: Unlike complex enterprise RAG platforms, this is **designed for individuals** who want to organize their knowledge, learn more effectively, and have smarter conversations with AI. Perfect for students, researchers, writers, and knowledge workers.

**💰 Sustainable Business Model**: The $9.99 Pro plan hits the sweet spot for individual users - affordable enough for personal use, valuable enough to convert engaged free users, and profitable enough to sustain ongoing development and infrastructure costs.

This architecture provides a robust, scalable foundation for your mobile RAG application with comprehensive user management, project-based knowledge organization, and seamless chat experiences. The design ensures security, performance, and cost-effectiveness while maintaining excellent user experience through async processing and real-time updates.