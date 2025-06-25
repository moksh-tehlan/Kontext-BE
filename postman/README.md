# 📋 Kontext API Postman Collection

Complete Postman collection for testing all Kontext API endpoints with JWT authentication, Redis session management, UUID entity IDs, and enhanced security features.

## 📁 Files

- `Kontext-API.postman_collection.json` - Main API collection
- `Kontext-Development.postman_environment.json` - Development environment
- `Kontext-Production.postman_environment.json` - Production environment

## 🚀 Getting Started

### 1. Import Collection
1. Open Postman
2. Click **Import**
3. Select `Kontext-API.postman_collection.json`

### 2. Import Environment
1. Click **Import** again
2. Select the environment file:
   - `Kontext-Development.postman_environment.json` for local testing
   - `Kontext-Production.postman_environment.json` for production

### 3. Select Environment
- Click environment dropdown (top right)
- Select **Kontext - Development** or **Kontext - Production**

## 🔐 Authentication Flow (Login/Register)

### Step 1: Send OTP (Works for both new and existing users)
```
POST /api/auth/send-otp
{
  "email": "test@example.com"
}
```

### Step 2: Login/Register with OTP
```
POST /api/auth/login
{
  "email": "test@example.com",
  "otp": "123456"
}
```
**For new users**: Automatically creates account and logs in  
**For existing users**: Simply logs in  
**Auto-saves tokens to environment variables**

### Step 3: Use Protected Endpoints
All subsequent requests automatically use the stored access token.

## 📚 API Endpoints

### 🔑 Authentication
- **Send OTP (Login/Register)** - `POST /api/auth/send-otp`
- **Login/Register with OTP** - `POST /api/auth/login`
- **Google Login** - `POST /api/auth/google`
- **Refresh Token** - `POST /api/auth/refresh`
- **Logout** - `POST /api/auth/logout`

### 👥 User Management
- **Create User** - `POST /api/users`
- **Get All Users** - `GET /api/users` (Admin only)
- **Get User by ID** - `GET /api/users/{id}`
- **Get User by Email** - `GET /api/users/email/{email}`
- **Get Current User Profile** - `GET /api/users/me` ✨ New
- **Update User** - `PUT /api/users/{id}`
- **Update Current User Profile** - `PUT /api/users/me` ✨ New
- **Verify Email** - `PATCH /api/users/{id}/verify-email`
- **Update Role** - `PATCH /api/users/{id}/role` (Admin only)
- **Delete User** - `DELETE /api/users/{id}` (Admin only)

### 🔒 Security Testing ✨ New
- **Test Token Invalidation on Login** - Verify old tokens become invalid
- **Test Old Token Invalid After Login** - Confirm security enforcement
- **Test Token Refresh Invalidates Old Access Token** - Refresh security
- **Verify Old Access Token Invalid After Refresh** - Validation test

### 🏥 Health & Monitoring
- **Health Check** - `GET /actuator/health`
- **App Info** - `GET /actuator/info`
- **Metrics** - `GET /actuator/metrics`

## 🎯 Environment Variables

### Development Environment
```json
{
  "base_url": "http://localhost:8080/api/v1",
  "management_url": "http://localhost:8081",
  "user_email": "test@example.com"
}
```

### Production Environment
```json
{
  "base_url": "https://api.kontext.com/api/v1",
  "management_url": "https://management.kontext.com"
}
```

## 🧪 Automated Tests

Each request includes automated tests:
- ✅ Status code validation
- ✅ Response structure validation
- ✅ Token extraction and storage
- ✅ Response time checks
- ✅ Business logic validation

## 🔄 Token Management

### Automatic Token Handling
- **Login** → Stores `access_token` and `refresh_token`
- **Refresh** → Updates `access_token`
- **Logout** → Clears all tokens

### Manual Token Refresh
Use the "Refresh Token" request when access token expires.

## 🛠️ Customization

### Update Environment Variables
1. Select your environment
2. Click the eye icon 👁️
3. Edit variables as needed

### Common Variables to Update
- `user_email` - Your test email
- `base_url` - API base URL
- `otp_code` - OTP for testing

## 🚨 Security Notes

### Development
- Uses sample test data
- OTP code is pre-filled for testing

### Production
- **Never commit production tokens**
- Use secure OTP codes
- Validate all SSL certificates

## 📝 Usage Examples

### Testing Authentication Flow
1. **Send OTP** → Check email for OTP
2. **Login** → Tokens auto-saved
3. **Get Current User Profile** → Uses SecurityContextUtil
4. **Logout** → Clears tokens

### Testing Security Features ✨ New
1. **Login** → Get initial token
2. **Test Token Invalidation on Login** → Login again, old token stored
3. **Test Old Token Invalid** → Verify old token returns 401
4. **Test Token Refresh** → Get new access token
5. **Verify Old Access Token Invalid** → Confirm previous access token is invalid

### Testing Admin Functions
1. Login as admin user
2. **Get All Users** → Requires admin role
3. **Update User Role** → Requires admin role
4. **Delete User** → Requires admin role

### Testing Profile Management ✨ New
1. **Login** → Get tokens
2. **Get Current User Profile** → Uses `/me` endpoint
3. **Update Current User Profile** → Update using `/me` endpoint
4. **Verify Changes** → Check updated profile

## 🐛 Troubleshooting

### Common Issues

**401 Unauthorized**
- Check if access token is set
- Try refreshing token
- Re-login if refresh fails

**404 Not Found**
- Verify `base_url` in environment
- Check if application is running

**Connection Refused**
- Ensure Docker services are running:
  ```bash
  docker-compose up -d postgres redis
  ```

**Token Expired**
- Use "Refresh Token" request
- Or re-login with "Login with OTP"

## 🔗 Related Documentation
- [API Documentation](../docs/api.md)
- [Docker Setup](../docker/README.md)
- [Environment Configuration](../.env.example)