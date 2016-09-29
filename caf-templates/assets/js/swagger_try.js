(function() {

  'use strict';

  angular.module('caf').controller('SecurityCtrl', function SecurityCtrl($scope,
    $modal, AuthManager) {
    $scope.getHumanSecurityType = function(type) {
      var types = {
        basic: 'HTTP Basic Authentication',
        oauth2: 'OAuth 2.0',
        apiKey: 'API Key'
      };

      return types[type];
    };

    $scope.isAuthenticated = AuthManager.securityIsAuthenticated;

    $scope.authenticate = function(securityName, security) {
      if (security.type === 'basic') {
        $modal.open({
          templateUrl: 'auth/basic.html',
          controller: ['$scope', '$modalInstance',
          function BasicAuthAuthenticateCtrl($scope,
            $modalInstance) {
            $scope.cancel = $modalInstance.close;
            $scope.authenticate = function(username, password) {
              AuthManager.basicAuth(securityName, security, {
                username: username,
                password: password
              });
              $modalInstance.close();
            };
          }],
          size: 'large'
        });
      } else if (security.type === 'oauth2') {

        $modal.open({
          templateUrl: 'auth/oauth2.html',
          controller: ['$scope', '$modalInstance',

          function OAuth2AuthenticateCtrl($scope, $modalInstance) {
            $scope.cancel = $modalInstance.close;
            $scope.authenticate = function(accessToken) {
              if (!accessToken) {
                return;
              }
              AuthManager.oAuth2(securityName, security, {
                accessToken: accessToken
              });
              $modalInstance.close();
            };
          }],
          size: 'large'
        });
      } else if (security.type === 'apiKey') {
        $modal.open({
          templateUrl: 'auth/api-key.html',
          controller: ['$scope', '$modalInstance',
            function APIKeyAuthenticateCtrl($scope, $modalInstance) {
              $scope.cancel = $modalInstance.close;
              $scope.authenticate = function(apiKey) {
                if (!apiKey) {
                  return;
                }
                AuthManager.apiKey(securityName, security, {
                  apiKey: apiKey
                });
                $modalInstance.close();
              };
            }],
          size: 'large'
        });
      } else {
        window.customAlert('Not yet supported');
      }
    };
  });


  angular.module('caf').directive('schemaModel', function() {
    return {
      templateUrl: 'schema-model.html',
      restrict: 'E',
      replace: true,
      scope: {
        schema: '=',
        showLevels: '@?'
      },

      link: function postLink($scope, $element) {
        $scope.mode = 'schema';

        $scope.switchMode = function() {
          $scope.mode = $scope.mode === 'json' ? 'schema' : 'json';
        };

        var render = function() {
          var formatter = new JSONFormatter($scope.schema, $scope.showLevels !== undefined && $scope.showLevels !== null  ? $scope.showLevels : 1);
          $element.find('td.view.json').html(formatter.render());

          var schemaView = new JSONSchemaView($scope.schema, $scope.showLevels !== undefined && $scope.showLevels !== null  ? $scope.showLevels : 1);
          $element.find('td.view.schema').html(schemaView.render());
        };

        $scope.$watch('schema', render);

        render();
      }
    };
  });


    angular.module('caf').provider('$sessionStorage', _storageProvider('sessionStorage'));

    function _storageProvider(storageType) {
        return function() {
            var storageKeyPrefix = 'ngStorage-';

            this.setKeyPrefix = function(prefix) {
                if (typeof prefix !== 'string') {
                    throw new TypeError('[ngStorage] - ' + storageType + 'Provider.setKeyPrefix() expects a String.');
                }
                storageKeyPrefix = prefix;
            };

            var serializer = angular.toJson;
            var deserializer = angular.fromJson;

            this.setSerializer = function(s) {
                if (typeof s !== 'function') {
                    throw new TypeError('[ngStorage] - ' + storageType + 'Provider.setSerializer expects a function.');
                }

                serializer = s;
            };

            this.setDeserializer = function(d) {
                if (typeof d !== 'function') {
                    throw new TypeError('[ngStorage] - ' + storageType + 'Provider.setDeserializer expects a function.');
                }

                deserializer = d;
            };

            // Note: This is not very elegant at all.
            this.get = function(key) {
                return deserializer(window[storageType].getItem(storageKeyPrefix + key));
            };

            // Note: This is not very elegant at all.
            this.set = function(key, value) {
                return window[storageType].setItem(storageKeyPrefix + key, serializer(value));
            };

            this.$get = [
                '$rootScope',
                '$window',
                '$log',
                '$timeout',
                '$document',

                function(
                    $rootScope,
                    $window,
                    $log,
                    $timeout,
                    $document
                ) {
                    function isStorageSupported(storageType) {

                        // Some installations of IE, for an unknown reason, throw "SCRIPT5: Error: Access is denied"
                        // when accessing window.localStorage. This happens before you try to do anything with it. Catch
                        // that error and allow execution to continue.

                        // fix 'SecurityError: DOM Exception 18' exception in Desktop Safari, Mobile Safari
                        // when "Block cookies": "Always block" is turned on
                        var supported;
                        try {
                            supported = $window[storageType];
                        } catch (err) {
                            supported = false;
                        }

                        // When Safari (OS X or iOS) is in private browsing mode, it appears as though localStorage
                        // is available, but trying to call .setItem throws an exception below:
                        // "QUOTA_EXCEEDED_ERR: DOM Exception 22: An attempt was made to add something to storage that exceeded the quota."
                        if (supported && storageType === 'localStorage') {
                            var key = '__' + Math.round(Math.random() * 1e7);

                            try {
                                localStorage.setItem(key, key);
                                localStorage.removeItem(key);
                            } catch (err) {
                                supported = false;
                            }
                        }

                        return supported;
                    }

                    // The magic number 10 is used which only works for some keyPrefixes...
                    // See https://github.com/gsklee/ngStorage/issues/137
                    var prefixLength = storageKeyPrefix.length;

                    // #9: Assign a placeholder object if Web Storage is unavailable to prevent breaking the entire AngularJS app
                    var webStorage = isStorageSupported(storageType) || ($log.warn('This browser does not support Web Storage!'), {
                            setItem: angular.noop,
                            getItem: angular.noop,
                            removeItem: angular.noop
                        }),
                        $storage = {
                            $default: function(items) {
                                for (var k in items) {
                                    angular.isDefined($storage[k]) || ($storage[k] = angular.copy(items[k]));
                                }

                                $storage.$sync();
                                return $storage;
                            },
                            $reset: function(items) {
                                for (var k in $storage) {
                                    '$' === k[0] || (delete $storage[k] && webStorage.removeItem(storageKeyPrefix + k));
                                }

                                return $storage.$default(items);
                            },
                            $sync: function() {
                                for (var i = 0, l = webStorage.length, k; i < l; i++) {
                                    // #8, #10: `webStorage.key(i)` may be an empty string (or throw an exception in IE9 if `webStorage` is empty)
                                    (k = webStorage.key(i)) && storageKeyPrefix === k.slice(0, prefixLength) && ($storage[k.slice(prefixLength)] = deserializer(webStorage.getItem(k)));
                                }
                            },
                            $apply: function() {
                                var temp$storage;

                                _debounce = null;

                                if (!angular.equals($storage, _last$storage)) {
                                    temp$storage = angular.copy(_last$storage);
                                    angular.forEach($storage, function(v, k) {
                                        if (angular.isDefined(v) && '$' !== k[0]) {
                                            webStorage.setItem(storageKeyPrefix + k, serializer(v));
                                            delete temp$storage[k];
                                        }
                                    });

                                    for (var k in temp$storage) {
                                        webStorage.removeItem(storageKeyPrefix + k);
                                    }

                                    _last$storage = angular.copy($storage);
                                }
                            }
                        },
                        _last$storage,
                        _debounce;

                    $storage.$sync();

                    _last$storage = angular.copy($storage);

                    $rootScope.$watch(function() {
                        _debounce || (_debounce = $timeout($storage.$apply, 100, false));
                    });

                    // #6: Use `$window.addEventListener` instead of `angular.element` to avoid the jQuery-specific `event.originalEvent`
                    $window.addEventListener && $window.addEventListener('storage', function(event) {
                        if (!event.key) {
                            return;
                        }

                        // Reference doc.
                        var doc = $document[0];

                        if ((!doc.hasFocus || !doc.hasFocus()) && storageKeyPrefix === event.key.slice(0, prefixLength)) {
                            event.newValue ? $storage[event.key.slice(prefixLength)] = deserializer(event.newValue) : delete $storage[event.key.slice(prefixLength)];

                            _last$storage = angular.copy($storage);

                            $rootScope.$apply();
                        }
                    });

                    $window.addEventListener && $window.addEventListener('beforeunload', function() {
                        $storage.$apply();
                    });

                    return $storage;
                }
            ];
        };
    }



    angular.module('caf').filter('formdata', function() {
        return function formdata(object) {
            var result = [];

            if (angular.isObject(object)) {
                Object.keys(object).forEach(function(key) {
                    if (angular.isDefined(object[key])) {
                        result.push(key + ': ' + object[key]);
                    }
                });
            }

            return result.join('\n');
        };
    });

    angular.module('caf').service('AuthManager', function AuthManager($sessionStorage) {
        $sessionStorage.$default({
            securities: {}
        });

        /*
         * Authenticates HTTP Basic Auth securities
         * @param securityName {string} - name of the security
         * @param security {object} - the security object
         * @param options {object} - options of the security including authentication
         * details
         */
        this.basicAuth = function(securityName, security, options) {
            if (securityName === '$$hashKey') {
                return;
            }
            if (!_.isObject(options)) {
                throw new TypeError('Can not authenticate with options');
            }

            options.username = options.username || '';
            options.password = options.password || '';
            options.isAuthenticated = true;
            options.base64 = window.btoa(options.username + ':' + options.password);
            options.securityName = securityName;
            $sessionStorage.securities[securityName] = {
                type: 'basic',
                security: security,
                options: options
            };
        };

        /*
         * Authenticates OAuth2 securities
         * @param securityName {string} - name of the security
         * @param security {object} - the security object
         * @param options {object} - options of the security including authentication
         * details
         */
        this.oAuth2 = function(securityName, security, options) {
            if (securityName === '$$hashKey') {
                return;
            }
            options.isAuthenticated = true;
            $sessionStorage.securities[securityName] = {
                type: 'oAuth2',
                security: security,
                options: options
            };
        };

        /*
         * Authenticates API Key securities
         * @param securityName {string} - name of the security
         * @param security {object} - the security object
         * @param options {object} - options of the security including authentication
         * details
         */
        this.apiKey = function(securityName, security, options) {
            if (securityName === '$$hashKey') {
                return;
            }
            options.isAuthenticated = true;
            $sessionStorage.securities[securityName] = {
                type: 'apiKey',
                security: security,
                options: options
            };
        };

        /*
         * Gets a security object
         * @returns {object} the security object
         */
        this.getAuth = function(securityName) {
            return $sessionStorage.securities[securityName];
        };

        /*
         * Checks if a security is authenticated
         * @returns {boolean} - true if security is authenticated false otherwise
         */
        this.securityIsAuthenticated = function(securityName) {
            var auth = $sessionStorage.securities[securityName];

            return auth && auth.options && auth.options.isAuthenticated;
        };
    });


    angular.module('caf').provider('SchemaForm', function() {
        var options = angular.extend(JSONEditor.defaults.options, {
            'disable_edit_json': true,
            'disable_properties': true,
            'no_additional_properties': true,
            'disable_collapse': true
        });

        this.$get = function() {
            return {
                options: options
            };
        };

        this.setOptions = function(newOptions) {
            if (!angular.isObject(newOptions)) {
                throw new Error('options should be an object.');
            }

            angular.extend(options, newOptions);
        };

        this.getOptions = function() {
            return options;
        };
    });

    angular.module('caf').provider('JSONFormatterConfig', function JSONFormatterConfigProvider() {

        // Default values for hover preview config
        var hoverPreviewEnabled = false;
        var hoverPreviewArrayCount = 100;
        var hoverPreviewFieldCount = 5;

        return {
            get hoverPreviewEnabled() {
                return hoverPreviewEnabled;
            },
            set hoverPreviewEnabled(value) {
                hoverPreviewEnabled = !!value;
            },

            get hoverPreviewArrayCount() {
                return hoverPreviewArrayCount;
            },
            set hoverPreviewArrayCount(value) {
                hoverPreviewArrayCount = parseInt(value, 10);
            },

            get hoverPreviewFieldCount() {
                return hoverPreviewFieldCount;
            },
            set hoverPreviewFieldCount(value) {
                hoverPreviewFieldCount = parseInt(value, 10);
            },

            $get: function() {
                return {
                    hoverPreviewEnabled: hoverPreviewEnabled,
                    hoverPreviewArrayCount: hoverPreviewArrayCount,
                    hoverPreviewFieldCount: hoverPreviewFieldCount
                };
            }
        };
    });

    angular.module("caf").run(["$templateCache", function($templateCache) {$templateCache.put("json-formatter.html","<div ng-init=\"isOpen = open && open > 0\" class=\"json-formatter-row\"><a ng-click=\"toggleOpen()\"><span class=\"toggler {{isOpen ? \'open\' : \'\'}}\" ng-if=\"isObject()\"></span> <span class=\"key\" ng-if=\"hasKey\"><span class=\"key-text\">{{key}}</span><span class=\"colon\">:</span></span> <span class=\"value\"><span ng-if=\"isObject()\"><span class=\"constructor-name\">{{getConstructorName(json)}}</span> <span ng-if=\"isArray()\"><span class=\"bracket\">[</span><span class=\"number\">{{json.length}}</span><span class=\"bracket\">]</span></span></span> <span ng-if=\"!isObject()\" ng-click=\"openLink(isUrl)\" class=\"{{type}}\" ng-class=\"{date: isDate, url: isUrl}\">{{parseValue(json)}}</span></span> <span ng-if=\"showThumbnail()\" class=\"thumbnail-text\">{{getThumbnail()}}</span></a><div class=\"children\" ng-if=\"getKeys().length && isOpen\"><json-formatter ng-repeat=\"key in getKeys() track by $index\" json=\"json[key]\" key=\"key\" open=\"childrenOpen()\"></json-formatter></div><div class=\"children empty object\" ng-if=\"isEmptyObject()\"></div><div class=\"children empty array\" ng-if=\"getKeys() && !getKeys().length && isOpen && isArray()\"></div></div>");}]);


    angular.module('caf').directive('tryJsonFormatter', ['RecursionHelper', 'JSONFormatterConfig', function jsonFormatterDirective(RecursionHelper, JSONFormatterConfig) {
        function escapeString(str) {
            return str.replace('"', '\"');
        }

        // From http://stackoverflow.com/a/332429
        function getObjectName(object) {
            if (object === undefined) {
                return '';
            }
            if (object === null) {
                return 'Object';
            }
            if (typeof object === 'object' && !object.constructor) {
                return 'Object';
            }
            var funcNameRegex = /function (.{1,})\(/;
            var results = (funcNameRegex).exec((object).constructor.toString());
            if (results && results.length > 1) {
                return results[1];
            } else {
                return '';
            }
        }

        function getType(object) {
            if (object === null) {
                return 'null';
            }
            return typeof object;
        }

        function getValuePreview(object, value) {
            var type = getType(object);

            if (type === 'null' || type === 'undefined') {
                return type;
            }

            if (type === 'string') {
                value = '"' + escapeString(value) + '"';
            }
            if (type === 'function') {

                // Remove content of the function
                return object.toString()
                    .replace(/[\r\n]/g, '')
                    .replace(/\{.*\}/, '') + '{…}';

            }
            return value;
        }

        function getPreview(object) {
            var value = '';
            if (angular.isObject(object)) {
                value = getObjectName(object);
                if (angular.isArray(object))
                    value += '[' + object.length + ']';
            } else {
                value = getValuePreview(object, object);
            }
            return value;
        }

        function link(scope) {
            scope.isArray = function() {
                return angular.isArray(scope.json);
            };

            scope.isObject = function() {
                return angular.isObject(scope.json);
            };

            scope.getKeys = function() {
                if (scope.isObject()) {
                    return Object.keys(scope.json).map(function(key) {
                        if (key === '') {
                            return '""';
                        }
                        return key;
                    });
                }
            };
            scope.type = getType(scope.json);
            scope.hasKey = typeof scope.key !== 'undefined';
            scope.getConstructorName = function() {
                return getObjectName(scope.json);
            };

            if (scope.type === 'string') {

                // Add custom type for date
                if ((new Date(scope.json)).toString() !== 'Invalid Date') {
                    scope.isDate = true;
                }

                // Add custom type for URLs
                if (scope.json.indexOf('http') === 0) {
                    scope.isUrl = true;
                }
            }

            scope.isEmptyObject = function() {
                return scope.getKeys() && !scope.getKeys().length &&
                    scope.isOpen && !scope.isArray();
            };


            // If 'open' attribute is present
            scope.isOpen = !!scope.open;
            scope.toggleOpen = function() {
                scope.isOpen = !scope.isOpen;
            };
            scope.childrenOpen = function() {
                if (scope.open > 1) {
                    return scope.open - 1;
                }
                return 0;
            };

            scope.openLink = function(isUrl) {
                if (isUrl) {
                    window.location.href = scope.json;
                }
            };

            scope.parseValue = function(value) {
                return getValuePreview(scope.json, value);
            };

            scope.showThumbnail = function() {
                return !!JSONFormatterConfig.hoverPreviewEnabled && scope.isObject() && !scope.isOpen;
            };

            scope.getThumbnail = function() {
                if (scope.isArray()) {

                    // if array length is greater then 100 it shows "Array[101]"
                    if (scope.json.length > JSONFormatterConfig.hoverPreviewArrayCount) {
                        return 'Array[' + scope.json.length + ']';
                    } else {
                        return '[' + scope.json.map(getPreview).join(', ') + ']';
                    }
                } else {

                    var keys = scope.getKeys();

                    // the first five keys (like Chrome Developer Tool)
                    var narrowKeys = keys.slice(0, JSONFormatterConfig.hoverPreviewFieldCount);

                    // json value schematic information
                    var kvs = narrowKeys
                        .map(function(key) {
                            return key + ':' + getPreview(scope.json[key]);
                        });

                    // if keys count greater then 5 then show ellipsis
                    var ellipsis = keys.length >= 5 ? '…' : '';

                    return '{' + kvs.join(', ') + ellipsis + '}';
                }
            };
        }

        return {
            templateUrl: 'json-formatter.html',
            restrict: 'E',
            replace: true,
            scope: {
                json: '=',
                key: '=',
                open: '='
            },
            compile: function(element) {

                // Use the compile function from the RecursionHelper,
                // And return the linking function(s) which it returns
                return RecursionHelper.compile(element, link);
            }
        };
    }]);


    angular.module('caf').directive('swaggerOperation', function($parse) {
        return {
            restrict: 'E',
            replace: true,
            templateUrl: 'operation.html',
            scope: false,
            link: function($scope, element, attrs) {

                $scope.path = $parse(attrs.path)($scope);
                $scope.operation = $parse(attrs.operation)($scope);
                $scope.specs = $parse(attrs.schema)($scope);
                $scope.pathName = $parse(attrs.pathName)($scope);
                $scope.operationName = $parse(attrs.operationName)($scope);

                $scope.isTryOpen = false;
                $scope.toggleTry = function toggleTry() {
                    $scope.isTryOpen = !$scope.isTryOpen;
                };

                /*
                 * Gets all available parameters
                 *
                 * @returns {array} - array of parameters
                 */
                $scope.getParameters = function getParameters() {
                    var hasPathParameter = _.isArray($scope.path.parameters);
                    var hasOperationParameter = _.isArray($scope.operation.parameters);
                    var operationParameters = $scope.operation.parameters;
                    var pathParameters = $scope.path.parameters;

                    // if there is no operation and path parameter return empty array
                    if (!hasOperationParameter && !hasPathParameter) {
                        return [];
                    }

                    // if there is no operation parameter return only path parameters
                    if (!hasOperationParameter) {
                        operationParameters = [];
                    }

                    // if there is no path parameter return operation parameters
                    if (!hasPathParameter) {
                        pathParameters = [];
                    }

                    // if there is both path and operation parameters return all of them
                    return operationParameters.concat(pathParameters)
                        .map(setParameterSchema);
                };

                /**
                 * Sets the schema object for a parameter even if it doesn't have schema
                 *
                 * @param {object} parameter - parameter
                 * @return {object} sets the schema object
                 */
                function setParameterSchema(parameter) {
                    if (parameter.schema) {
                        return parameter;
                    } else if (parameter.type === 'array') {
                        parameter.schema = _.pick(parameter, 'type', 'items');
                    } else {
                        var schema = {
                            type: parameter.type
                        };

                        if (parameter.format) {
                            schema.format = parameter.format;
                        }

                        parameter.schema = schema;
                    }

                    // if allowEmptyValue is explicitly set to false it means this parameter
                    // is required for making a request.
                    if (parameter.allowEmptyValue === false) {
                        parameter.schema.required = true;
                    }

                    return parameter;
                }

                /**
                 * Returns true if the operation responses has at least one response with
                 * schema
                 *
                 * @param {object} responses - a hash of responses
                 * @return {boolean} true/false
                 */
                $scope.hasAResponseWithSchema = function(responses) {
                    return _.keys(responses).some(function(responseCode) {
                        return responses[responseCode] && responses[responseCode].schema;
                    });
                };

                /**
                 * Returns true if the operation responses has at least one response with
                 * "headers" field
                 *
                 * @param {object} responses - a hash of responses
                 * @return {boolean} - true/false
                 */
                $scope.hasAResponseWithHeaders = function(responses) {
                    return _.keys(responses).some(function(responseCode) {
                        return responses[responseCode] && responses[responseCode].headers;
                    });
                };

                /**
                 * Returns true if the operation responses has at least one response with
                 * examples
                 *
                 * @param {object} responses - a hash of responses
                 * @return {boolean} - true/false
                 */
                $scope.hasAResponseWithExamples = function(responses) {
                    return _.keys(responses).some(function(responseCode) {
                        return responses[responseCode] && responses[responseCode].examples;
                    });
                };
            }
        };
    });


    angular.module('caf').controller('TryOperation', function($scope, formdataFilter,
        AuthManager, SchemaForm) {

        var parameters = $scope.getParameters();
        var securityOptions = getSecurityOptions();

        // binds to $scope
        $scope.generateUrl = generateUrl;
        $scope.makeCall = makeCall;
        $scope.xhrInProgress = false;
        $scope.parameters = parameters;
        $scope.getRequestBody = getRequestBody;
        $scope.hasRequestBody = hasRequestBody;
        $scope.getHeaders = getHeaders;
        $scope.requestModel = makeRequestModel();
        $scope.requestSchema = makeRequestSchema();
        $scope.hasFileParam = hasFileParam();
        $scope.contentType = $scope.requestModel.contentType;

        // httpProtocol is static for now we can use HTTP2 later if we wanted
        $scope.httpProtocol = 'HTTP/1.1';
        $scope.locationHost = window.location.host;

        configureSchemaForm();

        // Deeply watch specs for updates to regenerate the from
        $scope.$watch('specs', function() {
            $scope.requestModel = makeRequestModel();
            $scope.requestSchema = makeRequestSchema();
        }, true);

        // JSON Editor options
        var defaultOptions = {
            /* eslint-disable */
            theme: 'bootstrap3',
            remove_empty_properties: true,
            show_errors: 'change'
        };

        var looseOptions = {
            no_additional_properties: false,
            disable_properties: false,
            disable_edit_json: false
                /*eslint-enable */
        };

        SchemaForm.options = defaultOptions;

        /**
         * configure SchemaForm directive based on request schema
         */
        function configureSchemaForm() {
            // Determine if this request has a loose body parameter schema
            // A loose body parameter schema is a body parameter that allows additional
            // properties or has no properties object
            //
            // Note that "loose schema" is not a formal definition, we use this
            // definition here to determine type of form to render
            var loose = false;

            // loose schema is only for requests with body parameter
            if (hasRequestBody()) {
                // we're accessing deep in the schema. many operations can fail here
                try {
                    var param = $scope.requestSchema.properties.parameters;
                    /* eslint guard-for-in: "error"*/
                    for (var p in param.properties) {
                        if (param.properties[p].in === 'body' &&
                            isLooseJSONSchema(param.properties[p])) {
                            loose = true;
                        }
                    }
                } catch (e) {}
            } else {
                loose = false;
            }

            SchemaForm.options = _.extend(defaultOptions, loose ? looseOptions : {});
        }

        /**
         * Determines if a JSON Schema is loose
         *
         * @param {object} schema - A JSON Schema object
         *
         * @return {boolean} true if schema is a loose JSON
         */
        function isLooseJSONSchema(schema) {
            // loose object
            if (schema.additionalProperties || _.isEmpty(schema.properties)) {
                return true;
            }

            // loose array of objects
            if (
                schema.type === 'array' &&
                (schema.items.additionalProperties ||
                    _.isEmpty(schema.items.properties))
            ) {
                return true;
            }

            return false;
        }

        /**
         * Appends JSON Editor options for schema recursively so if a schema needs to
         * be edited by JSON Editor loosely it's possible
         *
         * @param {object} schema - A JSON Schema object
         *
         * @return {object} - A JSON Schema object
         */
        function appendJSONEditorOptions(schema) {
            var looseOptions = {
                /*eslint-disable */
                no_additional_properties: false,
                disable_properties: false,
                disable_edit_json: false
                    /*eslint-enable */
            };

            // If schema is loose add options for JSON Editor
            if (isLooseJSONSchema(schema)) {
                schema.options = looseOptions;
            }

            _.each(schema.properties, appendJSONEditorOptions);

            return schema;
        }

        /**
         * Makes the request schema to generate the form in the template
         * The schema has all required attributes for making a call for this operation
         *
         * @return {object} - A JSON Schema containing all properties required to
         *   make this call
         */
        function makeRequestSchema() {
            // base schema
            var schema = {
                type: 'object',
                title: 'Request',
                required: ['scheme', 'accept'],
                properties: {
                    scheme: {
                        type: 'string',
                        title: 'Scheme',

                        // Add schemes
                        enum: walkToProperty('schemes')
                    },
                    accept: {
                        type: 'string',
                        title: 'Accept',

                        // All possible Accept headers
                        enum: walkToProperty('produces')
                    }
                }
            };

            // Only if there is a security definition add security property
            if (securityOptions.length) {
                schema.properties.security = {
                    title: 'Security',
                    description: 'Only authenticated security options are shown.',
                    type: 'array',
                    uniqueItems: true,
                    items: {
                        type: 'string',

                        // All security options
                        enum: securityOptions
                    }
                };
            }

            // Add Content-Type header only if this operation has a body parameter
            if (hasRequestBody()) {
                var defaultConsumes = [
                    'multipart/form-data',
                    'x-www-form-urlencoded',
                    'application/json'
                ];
                schema.properties.contentType = {
                    type: 'string',
                    title: 'Content-Type',
                    enum: walkToProperty('consumes') || defaultConsumes
                };
            }

            // Only if there is a parameter add the parameters property
            if (parameters.length) {
                schema.properties.parameters = {
                    type: 'object',
                    title: 'Parameters',
                    properties: {}
                };

                // Add a new property for each parameter
                parameters.map(pickSchemaFromParameter).map(normalizeJSONSchema)
                    .forEach(function(paramSchema) {
                        // extend the parameters property with the schema
                        schema.properties.parameters
                            .properties[paramSchema.name] = paramSchema;
                    });
            }
            return schema;
        }

        /**
         * Makes a model with empty values that conforms to the JSON Schema generated
         *   by makeRequestSchema.
         *
         * @return {object} - the model
         */
        function makeRequestModel() {
            // base model
            var model = {

                // Add first scheme as default scheme
                scheme: walkToProperty('schemes')[0],

                // Default Accept header is the first one
                accept: walkToProperty('produces')[0]
            };

            // if there is security options add the security property
            if (securityOptions.length) {
                model.security = securityOptions;
            }

            // Add Content-Type header only if this operation has a body parameter
            if (hasRequestBody()) {
                // Default to application/json
                model.contentType = 'application/json';
            }

            // Only if there is a parameter add the parameters default values
            if (parameters.length) {
                model.parameters = {};
                parameters.map(pickSchemaFromParameter).map(normalizeJSONSchema)
                    .forEach(function(paramSchema) {
                        var defaults = {
                            object: {},
                            array: [],
                            integer: 0,
                            string: ''
                        };

                        // if default value is provided use it
                        if (angular.isDefined(paramSchema.default)) {
                            model.parameters[paramSchema.name] = paramSchema.default;

                            // if there is no default value but there is minimum or maximum use them
                        } else if (angular.isDefined(paramSchema.minimum)) {
                            model.parameters[paramSchema.name] = paramSchema.minimum;
                        } else if (angular.isDefined(paramSchema.maximum)) {
                            model.parameters[paramSchema.name] = paramSchema.maximum;

                            // if there is no default value select a default value based on type
                        } else if (angular.isDefined(defaults[paramSchema.type])) {
                            var title = paramSchema.name || paramSchema.name;

                            if (paramSchema.type === 'object') {
                                model.parameters[title] = createEmptyObject(paramSchema);
                            } else {
                                model.parameters[title] = defaults[paramSchema.type];
                            }

                            // use empty string as fallback
                        } else {
                            model.parameters[paramSchema.name] = '';
                        }
                    });
            }

            return model;
        }

        /**
         * Resolves all of `allOf` recursively in a schema
         * @description
         * if a schema has allOf it means that the schema is the result of mergin all
         * schemas in it's allOf array.
         *
         * @param {object} schema - JSON Schema
         *
         * @return {object} JSON Schema
         */
        function resolveAllOf(schema) {
            if (schema.allOf) {
                schema = _.assign.apply(null, [schema].concat(schema.allOf));
                delete schema.allOf;
            }

            if (_.isObject(schema.properties)) {
                schema.properties = _.keys(schema.properties)
                    .reduce(function(properties, key) {
                        properties[key] = resolveAllOf(schema.properties[key]);
                        return properties;
                    }, {});
            }

            return schema;
        }

        /**
         * Fills in empty gaps of a JSON Schema. This method is mostly used to
         * normalize JSON Schema objects that are abstracted from Swagger parameters
         *
         * @param {object} schema - JSON Schema
         *
         * @return {object} - Normalized JSON Schema
         */
        function normalizeJSONSchema(schema) {
            // provide title property if it's missing.
            if (!schema.title && angular.isString(schema.name)) {
                schema.title = schema.name;
            }

            schema = resolveAllOf(schema);

            // if schema is missing the "type" property fill it in based on available
            // properties
            if (!schema.type) {
                // it's an object if it has "properties" property
                if (schema.properties) {
                    schema.type = 'object';
                }

                // it's an array if it has "items" property
                if (schema.items) {
                    schema.type = 'array';
                }
            }

            // Swagger extended JSON Schema with a new type, file. If we see file type
            // we will add format: file to the schema so the form generator will render
            // a file input
            if (schema.type === 'file') {
                schema.type = 'string';
                schema.format = 'file';
            }

            return appendJSONEditorOptions(schema);
        }

        /**
         * Because some properties are cascading this walks up the tree to get them
         *
         * @param {string} propertyName - property name
         *
         * @return {array|undefined} - list of possible properties
         */
        function walkToProperty(propertyName) {
            var defaultProperties = {
                produces: ['*/*'],
                schemes: ['http']
            };

            if (Array.isArray($scope.operation[propertyName])) {
                return $scope.operation[propertyName];
            } else if (Array.isArray($scope.specs[propertyName])) {
                return $scope.specs[propertyName];
            }

            // By default return the default property if it exists
            if (defaultProperties[propertyName]) {
                return defaultProperties[propertyName];
            }

            return undefined;
        }

        /**
         * Walks up the Swagger tree to find all possible security options
         *
         * @return {array} - a list of security options or an empty array
         */
        function getSecurityOptions() {
            var securityOptions = [];

            // operation level securities
            if (_.isArray($scope.operation.security)) {
                $scope.operation.security.forEach(function(security) {
                    _.keys(security).forEach(function(key) {
                        securityOptions = securityOptions.concat(key);
                    });
                });

                // root level securities
            } else if (_.isArray($scope.specs.security)) {
                $scope.specs.security.forEach(function(security) {
                    _.keys(security).forEach(function(key) {
                        securityOptions = securityOptions.concat(key);
                    });
                });
            }

            return _.uniq(securityOptions).filter(function(security) {
                // only return authenticated options
                return AuthManager.securityIsAuthenticated(security);
            });
        }

        /**
         * Picks JSON Schema from parameter
         * Since the parameter is a subset of JSON Schema we need to add
         * the missing properties
         *
         * @param {object} parameter - the parameter
         * @return {object} - the schema
         */
        function pickSchemaFromParameter(parameter) {
            // if parameter has a schema populate it into the parameter so the parameter
            // has all schema properties
            if (parameter.schema) {
                return _.omit(_.extend(parameter, parameter.schema), 'schema');

                // if parameter does not have a schema, use the parameter itself as
                // schema.
            }
            return parameter;
        }

        /**
         * Creates empty object from JSON Schema
         *
         * @param {object} schema - JSON Schema
         *
         * @return {object} - result (empty object based on the schema)
         */
        function createEmptyObject(schema) {
            if (schema.type !== 'object') {
                throw new TypeError('schema should be an object schema.');
            }

            // (TODO) expand this list
            var defaultValues = {
                string: '',
                integer: 0
            };

            var result = {};

            // If schema has no properties (loose schema), return the empty object
            if (!schema.properties) {
                return result;
            }

            Object.keys(schema.properties).forEach(function(propertyName) {
                // if this property is an object itself, recurse
                if (schema.properties[propertyName].type === 'object') {
                    result[propertyName] =
                        createEmptyObject(schema.properties[propertyName]);

                    // otherwise use the defaultValues hash
                } else {
                    result[propertyName] =
                        defaultValues[schema.properties[propertyName].type] || null;
                }
            });

            return result;
        }

        /**
         * Generates a filter function based on type for filtering parameters
         *
         * @param {string} type - type of parameter
         *
         * @return {function} - the filter function
         */
        function parameterTypeFilter(type) {
            return function filterParams(parameter) {
                return parameter.in === type;
            };
        }

        /**
         * Used for generating a hash from array of parameters.
         *   This method is used in Array#reduce method iterations
         *
         * @param {object} hash - the hash passed around in iterations
         * @param {object} param - a Swagger parameter object
         *
         * @return {object} - complete hash from parameters to this iterations
         */
        function hashifyParams(hash, param) {
            if (!hash) {
                hash = {};
            }

            var paramValue = $scope.requestModel.parameters[param.name];
            var required = $scope.requestSchema.properties.parameters
                .properties[param.name].required === true;

            // if this parameter is not provided (undefined or empty string value) by
            // user and it's not required, move to next parameter without adding
            // this one to the hash
            if (!required) {
                if (paramValue === undefined) {
                    return hash;
                }
                if (param.type === 'string' && paramValue === '') {
                    return hash;
                }
            }

            hash[param.name] = $scope.requestModel.parameters[param.name];

            return hash;
        }

        /**
         * Generates the URL for this call based on all parameters and other
         *   conditions
         *
         * @return {string} - the URL
         */
        function generateUrl() {
            var requestModel = $scope.requestModel;
            var scheme = requestModel.scheme;
            var host = $scope.specs.host || window.location.host;
            var basePath = $scope.specs.basePath || '';
            var pathParams = parameters.filter(parameterTypeFilter('path'))
                .reduce(hashifyParams, {});
            var queryParams = parameters.filter(parameterTypeFilter('query'))
                .reduce(hashifyParams, {});
            var queryParamsStr;
            var pathStr;
            var isCollectionQueryParam = parameters.filter(parameterTypeFilter('query'))
                .some(function(parameter) {
                    // if a query parameter has a collection format it doesn't matter what
                    // is it's value, it will force the URL to not use `[]` in query string
                    return parameter.items && parameter.items.collectionFormat;
                });

            // a regex that matches mustaches in path. e.g: /{pet}
            var pathParamRegex = /{([^{}]+)}/g;

            // if basePath is just a single slash (`/`), ignore it
            if (basePath === '/') {
                basePath = '';
            }

            // if there are selected securities and they are located in the query append
            // them to the URL
            if (angular.isArray(requestModel.security)) {
                requestModel.security.forEach(function(securityOption) {
                    var auth = AuthManager.getAuth(securityOption);

                    // if auth exists and it's an api key in query, add it to query params
                    if (auth && auth.type === 'apiKey' && auth.security.in === 'query') {
                        var authQueryParam = {};
                        authQueryParam[auth.security.name] = auth.options.apiKey;
                        _.extend(queryParams, authQueryParam);
                    }
                });
            }

            // generate the query string portion of the URL based on query parameters
            queryParamsStr = window.decodeURIComponent(
                $.param(queryParams, isCollectionQueryParam));

            // fill in path parameter values inside the path
            pathStr = $scope.pathName.replace(pathParamRegex,

                // a simple replace method where it uses the available path parameter
                // value to replace the path parameter or leave it as it is if path
                // parameter doesn't exist.
                function(match) {
                    var matchKey = match.substring(1, match.length - 1);

                    if (angular.isDefined(pathParams[matchKey])) {
                        return pathParams[matchKey];
                    }

                    return match;
                }
            );

            // queryParamsStr can be undefined. Fall back to empty string in that case
            queryParamsStr = queryParamsStr ? ('?' + queryParamsStr) : '';

            // constructing the URL
            return scheme + '://' + // example: http://
                host + // example: api.example.com
                basePath + // example: /v1
                pathStr + // example: /users/me
                queryParamsStr; // example: ?all=true
        }

        /*
         * Returns all header parameters
         *
         * @returns {object} - list of all parameters that are in header
         */
        var getHeaderParams = function() {
            // Select header parameters from all parameters and reduce them into a
            // single key/value hash where the key is parameter name
            var params = parameters.filter(parameterTypeFilter('header'))
                .reduce(hashifyParams, {});

            // add header based securities to list of headers
            if (angular.isArray($scope.requestModel.security)) {
                $scope.requestModel.security.forEach(function(secuirtyOption) {
                    var auth = AuthManager.getAuth(secuirtyOption);

                    if (auth) {
                        var authHeader = {};

                        // HTTP basic authentication is always in header
                        if (auth.type === 'basic') {
                            authHeader = {
                                Authorization: 'Basic ' + auth.options.base64
                            };

                            // apiKey security can be in header, if it's in header use it
                        } else if (auth.type === 'apiKey' && auth.security.in === 'header') {
                            authHeader[auth.security.name] = auth.options.apiKey;

                            // OAuth securities are always in header
                        } else if (auth.type === 'oAuth2') {
                            authHeader = {
                                Authorization: 'Bearer ' + auth.options.accessToken
                            };
                        }

                        // Extend the params hash with this auth
                        params = _.extend(params, authHeader);
                    }
                });
            }

            return params;
        };

        /**
         * Returns all headers needed to be shown in request preview
         *
         * @return {object} - a hash of headers key/value pairs
         */
        function getHeaders() {
            var headerParams = getHeaderParams();
            var content = $scope.getRequestBody();

            // get spec host or default host in the window. remove port from Host header
            var host = ($scope.specs.host || window.location.host).replace(/:.+/, '');

            // A list of default headers that will be included in the XHR call
            var defaultHeaders = {
                'Host': host,
                'Accept': $scope.requestModel.accept || '*/*',
                'Accept-Encoding': 'gzip,deflate,sdch', // (TODO) where this is coming from?
                'Accept-Language': 'en-US,en;q=0.8,fa;q=0.6,sv;q=0.4', // (TODO) wut?
                'Cache-Control': 'no-cache',
                'Connection': 'keep-alive',
                'Origin': window.location.origin,
                'Referer': window.location.origin + window.location.pathname,
                'User-Agent': window.navigator.userAgent
            };

            headerParams = _.extend(defaultHeaders, headerParams);

            // if request has a body add Content-Type and Content-Length headers
            if (content !== null) {
                // (TODO) handle file case
                headerParams['Content-Length'] = content.length;
                headerParams['Content-Type'] = $scope.requestModel.contentType;
            }

            return headerParams;
        }

        /**
         * Determines if request has a body. A request has body if it has a parameter
         *  that is in body or in form data
         *
         * @return {boolean} - true if request has a body
         */
        function hasRequestBody() {
            var bodyParam = parameters.filter(parameterTypeFilter('body'));
            var formDataParams = parameters.filter(parameterTypeFilter('formData'));

            return bodyParam.length || formDataParams.length;
        }

        /**
         * Gets the body parameter's current value
         *
         * @return {string|object|null} - body parameter value or null if there is
         *   request body
         */
        function getBodyModel() {
            if (!hasRequestBody()) {
                return null;
            }

            var bodyParam = _.find(parameters, parameterTypeFilter('body'));
            var formDataParams = parameters.filter(parameterTypeFilter('formData'));

            // body parameter case
            if (bodyParam) {
                var bodyParamName = bodyParam.name;
                var bodyParamValue = $scope.requestModel.parameters[bodyParamName];

                return bodyParamValue;
            }

            // formData case
            return formDataParams.reduce(hashifyParams, {});
        }

        /**
         * Gets the request body based on current form data and other parameters
         *
         * @return {string|null} - Raw request body or null if there is no body model
         */
        function getRequestBody() {
            var bodyModel = getBodyModel();
            var contentType = $scope.requestModel.contentType;

            // if bodyModel doesn't exists, don't make a request body
            if (bodyModel === undefined || bodyModel === null) {
                return null;
            }

            // if encoding is not defined, return body model as is
            if (!contentType) {
                return bodyModel;

                // if body has form-data encoding use formdataFilter to encode it to string
            } else if (/form\-data/.test(contentType)) {
                return formdataFilter(bodyModel);

                // if body has application/json encoding use JSON to stringify it
            } else if (/json/.test(contentType)) {
                return JSON.stringify(bodyModel, null, 2);

                // if encoding is x-www-form-urlencoded use jQuery.param method to stringify
            } else if (/urlencode/.test(contentType)) {
                return $.param(bodyModel);
            }

            return null;
        }

        /**
         * Returns true if this operation has a body param and that body param has
         *  a file
         *
         * @return {boolean} true/false
         */
        function hasFileParam() {
            return parameters.some(function(parameter) {
                return parameter.format === 'file';
            });
        }

        /*
         * Parse a HTTP response header string into hash of HTTP header key/values
         * into
         *
         * @headers {string} - HTTP Headers
         *
         * @return {object} - HTTP header key/value
         */
        var parseHeaders = function(headers) {
            var result = {};

            headers.split('\n').forEach(function(line) {
                var key = line.split(':')[0];
                var value = line.split(':')[1];
                if (key && angular.isString(key) && angular.isString(value)) {
                    result[key.trim()] = value.trim();
                }
            });

            return result;
        };

        /**
         * Makes the XHR call
         *
         */
        function makeCall() {
            $scope.xhrInProgress = true;
            $scope.error = null;
            var omitHeaders = ['Host', 'Accept-Encoding', 'Connection', 'Origin',
                'Referer', 'User-Agent', 'Cache-Control', 'Content-Length'
            ];

            $.ajax({
                url: $scope.generateUrl(),
                type: $scope.operationName,
                headers: _.omit($scope.getHeaders(), omitHeaders),
                data: $scope.getRequestBody(),
                contentType: $scope.contentType
            })

            .fail(function(jqXHR, textStatus, errorThrown) {
                $scope.xhrInProgress = false;
                $scope.textStatus = textStatus;
                $scope.statusCode = jqXHR.status;
                $scope.xhr = jqXHR;

                if(textStatus === 'parsererror') {
        					$scope.textStatus = 'NOCONTENT';
        				}
                else if (errorThrown) {
                    $scope.error = errorThrown;
                } else if (textStatus === 'error') {
                    $scope.error = 'Server not found or an error occurred';
                }

                $scope.$digest();
            })

            .done(function(data, textStatus, jqXHR) {
                $scope.textStatus = textStatus;
                $scope.statusCode = jqXHR.status;
                $scope.xhrInProgress = false;
                $scope.responseData = data;
                $scope.xhr = jqXHR;
                $scope.responseHeaders = parseHeaders(jqXHR.getAllResponseHeaders());

                $scope.$digest();
            });
        }

        /**
         * Make pretty printed version of a JSON string
         *
         * @param {string} input - input
         *
         * @return {string} - printed version of a JSON string
         */
        $scope.prettyPrint = function(input) {
            // Try if it's JSON and return pretty JSON
            try {
                return JSON.stringify(JSON.parse(input), null, 2);
            } catch (jsonError) {}

            return input;
        };

        /**
         *
         * @param {string|object|array} value - response
         *
         * @return {boolean} true if response is JSON
         */
        $scope.isJson = function(value) {
            // if value is already parsed return true
            if (angular.isObject(value) || angular.isArray(value)) {
                return true;
            }

            var err;
            try {
                JSON.parse(value);
            } catch (error) {
                err = error;
            }

            return !err;
        };

        /**
         *
         * @param {object} headers - response headers
         * @param {string} type - the type to check for
         *
         * @return {boolean} true if response is specified type
         */
        $scope.isType = function(headers, type) {
            var regex = new RegExp(type);
            headers = headers || {};

            return headers['Content-Type'] && regex.test(headers['Content-Type']);
        };

        /**
         *
         * @return {boolean} true if this call is cross-origin
         */
        $scope.isCrossOrigin = function() {
            return $scope.specs.host && $scope.specs.host !== $scope.locationHost;
        };
    });

})();
