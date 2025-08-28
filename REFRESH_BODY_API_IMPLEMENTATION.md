# RefreshBody API Implementation

## Summary
This document describes the implementation of the new `refreshBody` API endpoint as requested in the problem statement.

## API Endpoint
```
POST /content/v3/refresh/body/:identifier
```

## Controller Method
```scala
controllers.v3.ContentController.refreshBody(identifier:String)
```

## Implementation Details

### 1. Constants Added
- **ContentConstants.scala**: Added `REFRESH_BODY = "refresh-body"` constant
- **ApiId.scala**: Added `REFRESH_BODY_CONTENT = "api.content.refresh.body"` API ID

### 2. Route Configuration
Added to `routes` file:
```
POST     /content/v3/refresh/body/:identifier        controllers.v3.ContentController.refreshBody(identifier:String)
```

### 3. Controller Implementation
The `refreshBody` method in `ContentController.scala` is identical to the `publish` method except:
- Calls `refreshBodyContent` operation instead of `publishContent`
- Uses `REFRESH_BODY_CONTENT` API ID

### 4. Actor Implementation
The `refreshBodyContent` method in `ContentActor.scala` is identical to `publishContent` except:
- Calls `PublishManager.refreshBody` instead of `PublishManager.publish`

### 5. PublishManager Updates
- Added `refreshBody` method that calls `publishWithAction` with `REFRESH_BODY` action
- Refactored existing `publish` method to use `publishWithAction` with `PUBLISH` action  
- Modified Kafka event generation to use configurable action parameter

### 6. Key Difference in Kafka Event
The only functional difference between `publish` and `refreshBody` APIs is in the Kafka event edata:

**Publish API:**
```scala
edata.put(ContentConstants.ACTION, ContentConstants.PUBLISH) // "publish"
```

**RefreshBody API:**
```scala
edata.put(ContentConstants.ACTION, ContentConstants.REFRESH_BODY) // "refresh-body"
```

## Request Format
The refreshBody API accepts the same request format as the publish API:

```json
{
  "request": {
    "content": {
      "lastPublishedBy": "userId"
    }
  }
}
```

## Response Format
The response format is identical to publish API except for the success message:

**Publish API Response:**
```json
{
  "publishStatus": "Publish Event for Content Id 'identifier' is pushed Successfully!"
}
```

**RefreshBody API Response:**
```json
{
  "publishStatus": "Refresh Body Event for Content Id 'identifier' is pushed Successfully!"
}
```

## Testing
Added test case in `ContentSpec.scala`:
```scala
"return success response for refreshBody API" in {
    val controller = app.injector.instanceOf[controllers.v3.ContentController]
    val result = controller.refreshBody("0123")(FakeRequest())
    isOK(result)
    status(result) must equalTo(OK)
}
```

## Usage Example
```bash
curl -X POST "http://localhost:9000/content/v3/refresh/body/do_12345" \
  -H "Content-Type: application/json" \
  -d '{
    "request": {
      "content": {
        "lastPublishedBy": "test-user"
      }
    }
  }'
```

## Validation
The implementation ensures that:
1. ✅ The API is completely identical to the publish API in functionality
2. ✅ The only difference is the Kafka event action value
3. ✅ Error handling and validation remain the same
4. ✅ Request/response format is consistent
5. ✅ Code reuse is maximized through refactoring