#### Version Number
${version-number}

#### New Features
 - Added support for openSUSE based containers  
    Workers built on this version of the framework will use the updated container startup scripts which support TLS with openSUSE base images.

#### Bug Fixes
- Update testing so that if invalid regex is encountered when validating values, return false rather than throw an exception.  
  Previous versions of worker-testing would throw an exception is an invalid regular expression was encountered during validation of simple values. This has been changed to consider this as not a match instead.
    
#### Known Issues
 - None
