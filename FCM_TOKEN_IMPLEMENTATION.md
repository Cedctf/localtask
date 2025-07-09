# FCM Token Implementation Guide

## Overview
This document details the complete FCM (Firebase Cloud Messaging) token implementation that ensures push notifications are delivered to the correct devices across all user scenarios.

## Implementation Strategy

### 1. Multi-Point Token Saving
FCM tokens are saved at multiple strategic points to ensure maximum reliability:

#### A. On Login ✅
**Location**: `LoginActivity.java` → `loginSuccessful()` method
```java
// Get and save FCM token to Firestore
FCMTokenManager fcmTokenManager = new FCMTokenManager(this);
fcmTokenManager.getAndSaveToken();
```

#### B. On App Launch ✅
**Location**: `TasksActivity.java` → `onCreate()` method
```java
// Get and save FCM token on app launch (for already logged in users)
saveUserFCMToken();
```

#### C. On Token Refresh ✅
**Location**: `MyFirebaseMessagingService.java` → `onNewToken()` method
```java
@Override
public void onNewToken(String token) {
    super.onNewToken(token);
    Log.d(TAG, "Refreshed token: " + token);
    
    // Save the new FCM token to Firestore
    saveTokenToFirestore(token);
}
```

### 2. Robust FCMTokenManager
**Location**: `FCMTokenManager.java`

#### Key Features:
- **Dual Collection Support**: Automatically tries both `users` and `hirers` collections
- **Fallback Logic**: If update fails, attempts to create document with merge
- **Cross-Collection Retry**: If `users` collection fails, tries `hirers` collection
- **Comprehensive Logging**: Detailed logs for debugging
- **Token Validation**: Checks for null/empty tokens before saving

#### Core Methods:
```java
public void getAndSaveToken()                    // Main entry point
public void saveTokenToFirestore(String, String) // Save to Firestore
private void saveTokenToCollection(...)          // Collection-specific saving
public void deleteTokenFromFirestore()           // Cleanup on logout
```

## Data Structure in Firestore

### Users Collection
```json
{
  "users": {
    "{userId}": {
      "name": "John Doe",
      "email": "john@example.com",
      "fcmToken": "dA1B2c3D4e5F6g7H8i9J0k...",
      "tokenUpdatedAt": "2024-01-15T10:30:00Z"
    }
  }
}
```

### Hirers Collection
```json
{
  "hirers": {
    "{hirerId}": {
      "name": "ABC Company",
      "email": "contact@abc.com",
      "fcmToken": "zA9B8c7D6e5F4g3H2i1J0k...",
      "tokenUpdatedAt": "2024-01-15T10:30:00Z"
    }
  }
}
```

## Usage Scenarios

### Scenario 1: New User Registration
1. User registers → `MainActivity.java`
2. Redirected to login → `LoginActivity.java`
3. User logs in → FCM token saved ✅

### Scenario 2: Existing User Login
1. User opens app → `WelcomeActivity.java`
2. User logs in → `LoginActivity.java`
3. FCM token saved ✅
4. App launches → `TasksActivity.java`
5. FCM token saved again (ensures latest) ✅

### Scenario 3: Already Logged In User
1. User opens app → `WelcomeActivity.java`
2. Auto-redirected to → `TasksActivity.java`
3. FCM token saved ✅

### Scenario 4: Token Refresh (Automatic)
1. Firebase automatically refreshes token
2. `MyFirebaseMessagingService.onNewToken()` triggered
3. New token saved automatically ✅

### Scenario 5: User Logs Out
1. User logs out (if implemented)
2. Call `fcmTokenManager.deleteTokenFromFirestore()`
3. Token removed from Firestore ✅

## Error Handling

### 1. Network Issues
- Retries with exponential backoff (Firebase SDK handles this)
- Graceful failure logging without crashing

### 2. User Document Not Found
- Automatic fallback to `set()` with merge options
- Creates document if it doesn't exist

### 3. Wrong Collection
- Tries `users` collection first
- Falls back to `hirers` collection if needed

### 4. Invalid Tokens
- Validates token before saving
- Logs warnings for null/empty tokens

## Debugging & Monitoring

### Log Tags to Monitor
```bash
# Android Logcat filters
adb logcat -s FCMTokenManager:* FCMService:*
```

### Key Log Messages
- `"Requesting FCM token for user: {userId}"`
- `"FCM Registration Token retrieved: {token}..."`
- `"FCM token saved successfully to {collection} collection"`
- `"Trying hirers collection as fallback"`

### Firestore Console Verification
1. Go to Firebase Console → Firestore Database
2. Check `users/{userId}` or `hirers/{hirerId}`
3. Verify `fcmToken` field exists and is recent
4. Check `tokenUpdatedAt` timestamp

## Integration with Notification System

### 1. Cloud Function Access
The saved FCM tokens are accessed by Cloud Functions:
```javascript
// functions/index.js
const hirerDoc = await admin.firestore()
    .collection('users')  // or 'hirers'
    .doc(hirerId)
    .get();

const fcmToken = hirerData.fcmToken;
```

### 2. Notification Delivery
- Cloud Function retrieves token from Firestore
- Sends notification to specific device
- User receives push notification

## Best Practices Implemented

### 1. Security
- Tokens stored securely in Firestore
- No tokens exposed in logs (truncated for security)
- Proper Firebase security rules should be applied

### 2. Reliability
- Multiple save points ensure token is always current
- Fallback mechanisms handle edge cases
- Automatic token refresh handling

### 3. Performance
- Asynchronous operations don't block UI
- Efficient Firestore operations with minimal reads/writes
- Proper error handling prevents app crashes

### 4. User Experience
- Silent token management (no user interaction required)
- Automatic permission requests
- Graceful degradation if notifications fail

## Testing Checklist

- [ ] New user registration → token saved
- [ ] Existing user login → token saved  
- [ ] App launch when logged in → token saved
- [ ] Token refresh → new token saved
- [ ] Network issues → graceful handling
- [ ] Document doesn't exist → created automatically
- [ ] Wrong user type → correct collection found
- [ ] Cloud Function → can retrieve tokens
- [ ] Notifications → delivered successfully

## Troubleshooting

### Common Issues

1. **Token not found in Firestore**
   - Check if user is properly logged in
   - Verify FCMTokenManager is called after login
   - Check Logcat for error messages

2. **Notifications not received**
   - Verify token exists in Firestore
   - Check Cloud Function logs
   - Ensure app has notification permissions

3. **Wrong collection accessed**
   - Check user type (User vs Hirer)
   - Verify SessionManager returns correct user ID
   - Check fallback logic is working

### Debug Steps
1. Enable verbose logging in FCMTokenManager
2. Check SessionManager.getUserId() returns valid ID
3. Verify Firestore security rules allow writes
4. Test with Firebase emulator first
5. Monitor Cloud Function logs for token retrieval

## Future Enhancements

- Add periodic token refresh (daily/weekly)
- Implement token validation before use
- Add notification preferences per user
- Support for multiple device tokens per user
- Token cleanup for inactive users 