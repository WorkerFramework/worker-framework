#### Version Number
${version-number}

#### New Features

 - [CAF-2334](https://jira.autonomy.com/browse/CAF-2334): JavaScript-based configuration  
    The Worker Framework has been enhanced to support using JavaScript as a configuration language.  This allows for more complex configuration than is possible via JSON files (and also allows the configuration file to contain comments!).

 - [CAF-2334](https://jira.autonomy.com/browse/CAF-2334): Configuration via Environment Variables  
    Building on the support for using JavaScript as a configuration language, it is possible to provide configuration files which delegate to environment variables.  Such files can be embedded into the worker containers so that workers can be configured using environment variables alone.  

    The Example Worker has been updated to demonstrate this capability, and the Worker Archetype has been updated to make this functionality readily available when building new workers.

 - [CAF-2375](https://jira.autonomy.com/browse/CAF-2375): Predictable testcase generation  
    The Worker Testing Framework now generates testcase files in a predictable order.  At most levels it is alphabetical but at the top level the order is dictated so that `inputData` will still come before `expectedOutput`.  This functionality makes it much easier to update testcases when there are changes to the expected output.

 - Improved logging in the Worker Testing Framework  
    The Worker Testing Framework feedback has been improved to provide more information while it is running, especially if it is abandoning waiting for expected messages.

 - [CAF-2306](https://jira.autonomy.com/browse/CAF-2306): Custom validation support  
    The Worker Testing Framework now supports custom validators to allow specific workers to provide their own validation logic when comparing expected and actual output.

#### Known Issues
 - None
