Merge Endpoint Plugin
=====================

This plugins adds a new rest endpoint which allows admins to merge branches via REST.
Any errors are returned in the response.

Request Example
----------------
```bash
curl -H "Content-Type: application/json" -X GET "http://localhost:7990/bitbucket/rest/merge/1.0/projects/PROJECT_1/repos/rep_1/merge?user=admin&comment=foo&fromBranch=123&toBranch=abc"
```

Error Response Example
-----------------------
```json
{"error":"The specified \"from\" branch, 123, does not exist"}
```

TODO: 
* Add some tests
* Better error handling
* Icon & Plugin description


