# Competency Framework Validation Implementation

## Overview

This implementation adds validation for Competency Framework hierarchies when calling the review and publish APIs. The validation ensures that Competency Framework nodes and their children (Competency Level nodes) conform to the specified schema requirements.

## APIs Affected

The validation is applied to the following endpoints:

### Content APIs
- `POST /content/v4/review/:identifier` (controllers.v4.ContentController.review)
- `POST /content/v4/publish/:identifier` (controllers.v4.ContentController.publish)

### Collection APIs  
- `POST /collection/v4/review/:identifier` (controllers.v4.CollectionController.review)
- `POST /collection/v4/publish/:identifier` (controllers.v4.CollectionController.publish)

## Implementation Details

### Files Modified/Added

1. **CompetencyFrameworkValidator.scala** (NEW)
   - Location: `content-api/content-actors/src/main/scala/org/sunbird/content/util/`
   - Contains the core validation logic for Competency Framework hierarchies

2. **ReviewManager.scala** (MODIFIED)
   - Added validation call before review processing
   - Only validates when primaryCategory = "Competency Framework"

3. **PublishManager.scala** (MODIFIED)
   - Added validation call before publish processing
   - Only validates when primaryCategory = "Competency Framework"

4. **TestCompetencyFrameworkValidator.scala** (NEW)
   - Location: `content-api/content-actors/src/test/scala/org/sunbird/content/util/`
   - Unit tests for the validation logic

### Validation Logic

The validation process works as follows:

1. **Trigger Check**: Only executes when the node's `primaryCategory` is "Competency Framework"
2. **Hierarchy Fetch**: Uses `HierarchyManager.getHierarchy()` to get the complete hierarchy
3. **Recursive Validation**: Validates each node in the hierarchy according to its type
4. **Error Handling**: Throws `ClientException` with descriptive error messages for validation failures

### Competency Framework Schema Validation

For nodes with `primaryCategory = "Competency Framework"`:

```json
{
  "visibility": "Private",              // REQUIRED: Must be "Private"
  "sector": {
    "name": "Education",               // REQUIRED: Must be "Education" 
    "domain": "Preschool"              // REQUIRED: Must be "Preschool"
  },
  "signupBy": "Admin|User",            // OPTIONAL: Must be "Admin" or "User"
  "enrollmentType": "Full Enrollment|Entrance Exam Based|Progress Based", // OPTIONAL
  "certificate": {
    "enabled": "Yes|No"                // OPTIONAL: Must be "Yes" or "No"
  }
}
```

### Competency Level Schema Validation

For nodes with `primaryCategory = "Competency Level"`:

```json
{
  "name": "string",                    // REQUIRED: Must be non-empty
  "visibility": "Parent",              // REQUIRED: Must be "Parent"
  "timeLimit": {
    "enabled": "Yes|No",               // OPTIONAL: Must be "Yes" or "No"
    "duration": {                      // REQUIRED when enabled = "Yes"
      "value": 1,                      // REQUIRED: Must be >= 1
      "unit": "Days|Months|Years"      // REQUIRED: Must be valid unit
    }
  },
  "entranceExam": {
    "enabled": "Yes|No",               // OPTIONAL: Must be "Yes" or "No"
    "collectionId": "string"           // REQUIRED when enabled = "Yes"
  },
  "levelExam": {
    "collectionId": "string",          // OPTIONAL
    "passingCriteria": {               // REQUIRED when collectionId provided
      "mustPass": "Yes|No"             // REQUIRED: Must be "Yes" or "No"
    }
  },
  "certificate": {
    "enabled": "Yes|No"                // OPTIONAL: Must be "Yes" or "No"
  }
}
```

### Error Codes

The implementation uses the following error codes:

- `ERR_HIERARCHY_NOT_FOUND`: Unable to fetch hierarchy for validation
- `ERR_COMPETENCY_FRAMEWORK_VALIDATION`: Validation errors for Competency Framework nodes
- `ERR_COMPETENCY_LEVEL_VALIDATION`: Validation errors for Competency Level nodes

### Example Validation Errors

```
ERR_COMPETENCY_FRAMEWORK_VALIDATION: Competency Framework visibility must be 'Private'
ERR_COMPETENCY_FRAMEWORK_VALIDATION: Competency Framework sector name must be 'Education'
ERR_COMPETENCY_LEVEL_VALIDATION: Competency Level name is required
ERR_COMPETENCY_LEVEL_VALIDATION: Competency Level visibility must be 'Parent'
ERR_COMPETENCY_LEVEL_VALIDATION: Competency Level timeLimit duration value must be at least 1
```

## Integration

The validation integrates seamlessly with the existing review/publish workflow:

1. **No Impact on Other Content Types**: Validation only runs for Competency Framework nodes
2. **Future-Compatible**: Uses existing error handling patterns and doesn't break existing functionality
3. **Comprehensive Coverage**: Validates the entire hierarchy, not just the root node
4. **Performance Conscious**: Only fetches hierarchy when necessary

## Testing

The implementation includes comprehensive test cases covering:

- Skipping validation for non-Competency Framework nodes
- Valid data scenarios
- Invalid visibility scenarios
- Invalid duration values
- Missing required fields
- Invalid enum values

## Example Request Flow

1. User calls `POST /content/v4/review/:identifier`
2. ContentController.review() → ContentActor.reviewContent()
3. ReviewManager.review() calls CompetencyFrameworkValidator.validateCompetencyFramework()
4. If primaryCategory = "Competency Framework":
   - Fetches hierarchy using HierarchyManager.getHierarchy()
   - Recursively validates each node in hierarchy
   - Throws ClientException if validation fails
5. If validation passes, continues with normal review process
6. Returns appropriate response to client

## Deployment Considerations

- No database schema changes required
- No configuration changes required
- Backward compatible with existing content
- Can be deployed without affecting existing functionality