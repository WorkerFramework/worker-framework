/*
  Initialise angular
*/
(function () {
    angular.module('caf', ['hpe.elements', 'RecursionHelper', 'mohsen1.schema-form', 'hc.marked']);
    angular.module('caf').directive('markdownify', markdownify);
    angular.module('caf').directive('table', table);
    angular.module('caf').directive('headerScroll', headerScroll);
    angular.module('caf').directive('hoverTooltip', hoverTooltip);
    angular.module('caf').directive('collapseButton', collapseButton);
    angular.module('caf').service('$swagger', $swagger);
    angular.module('caf').directive('jsonFormatter', jsonFormatter);
    angular.module('caf').directive('definition', definition);
    angular.module('caf').directive('menuToggle', menuToggle);
    angular.module('caf').directive('sideMenu', sideMenu);
    angular.module('caf').directive('responseCode', responseCode);
    angular.module('caf').directive('collapsiblePanel', collapsiblePanel);
    angular.module('caf').directive('ngHrefBind', ngHrefBind);
    angular.module('caf').directive('defaultUrl', defaultUrl);
    angular.module('caf').directive('openLink', openLink);
    angular.module('caf').directive('navbarButton', navbarButton);


    navbarButton.$inject = ['$timeout'];

    function navbarButton($timeout) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var innerLink = element.find('a');
                var current_url = window.location.href;

                $timeout(function() {

                    var nav_link = innerLink.attr('href');

                    if(!nav_link) {
                        return;
                    }

                    // if the string contains a # then we need to only take the left part of the string
                    if (current_url.indexOf('#') !== -1) {
                        current_url = current_url.substr(0, current_url.indexOf('#'));
                    }

                    if (endsWith(current_url, nav_link)) {
                        element.addClass('active');
                    }
                });

                // check if a string ends with a specific string
                function endsWith(subjectString, searchString, position) {

                    if (typeof position !== 'number' || !isFinite(position) || Math.floor(position) !== position || position > subjectString.length) {
                        position = subjectString.length;
                    }
                    position -= searchString.length;
                    var lastIndex = subjectString.indexOf(searchString, position);
                    return lastIndex !== -1 && lastIndex === position;
                }
            }
        };
    }

    function openLink() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                // get the target url
                var url = attrs.openLink;

                element.click(openNewTab);

                function openNewTab() {
                    var anchor = document.createElement('a');
                    anchor.setAttribute('href', url);

                    if (url && url.toLowerCase().indexOf('mailto') !== 0) {
                        anchor.setAttribute('target', '_blank');
                    }

                    anchor.style.display = 'none';
                    document.body.appendChild(anchor);

                    anchor.click();

                    document.body.removeChild(anchor);
                }
            }
        };
    }

    /*
        Automatically style tables
    */
    function table() {
        return {
            restrict: 'E',
            link: function (scope, element) {
                // get the native element
                var nativeElement = element.get(0);

                // check if element has table class already
                if (nativeElement.classList.contains('table')) return;

                // iterate each parent element
                var parent = nativeElement.parentElement;

                // check each parent to see if they have the swagger-container class
                while (parent) {

                    // if has class then stop here
                    if (parent.classList.contains('swagger-container')) return;

                    // check parent element
                    parent = parent.parentElement;
                }

                // if we get this far it is not in the swagger container - so add the class
                nativeElement.classList.add('table');
            }
        };
    }

    defaultUrl.$inject = ['$parse', '$localize'];

    function defaultUrl($parse, $localize) {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {

                // ensure a url has been provided
                if (!attrs.defaultUrl || attrs.defaultUrl === null) return;

                // get the default url
                var url = $parse(attrs.defaultUrl)(scope);

                // if no url then stop here
                if (!url) return;

                // get selected language if available
                var selected_language = $localize.getSelectedLanguage();

                if (url.hasOwnProperty(selected_language)) {
                    element.attr('href', baseUrl + '/' + url[selected_language]);
                } else {
                    // get the default language
                    var default_language = $localize.getDefaultLanguage();

                    element.attr('href', baseUrl + '/' + url[default_language]);
                }
            }
        };
    }

    /*
      Convert Markdown to html
    */
    function markdownify() {
        return {
            restrict: 'A',
            scope: {
                markdownify: '='
            },
            link: function (scope, element, attr) {

                // if there is an initial value then convert it
                if (scope.markdownify) {

                    // convert the markdown to html
                    var html = markdown.toHTML(scope.markdownify);

                    // replace the content of the element
                    element.html(html);
                }

                // watch for any changes
                scope.$watch('markdownify', function (nv, ov) {
                    // if the value is the same then do nothing
                    if (nv === ov) return;

                    // convert the markdown to html
                    var html = markdown.toHTML(scope.markdownify);

                    // replace the content of the element
                    element.html(html);
                });

            }
        };
    }

    /*
      Add shadow to header on scroll
    */
    function headerScroll() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {

                // store whether or not we are at the top
                var at_top = window.pageYOffset === 0;

                // bind to scroll event
                angular.element(window).scroll(updateClass);

                // set class initially
                updateClass();

                function updateClass() {

                    var offset = window.pageYOffset;

                    if (at_top && offset > 0) {
                        element.addClass('base-shadow');
                        at_top = false;
                    } else if (!at_top && offset === 0) {
                        element.removeClass('base-shadow');
                        at_top = true;
                    }
                }
            }
        };
    }


    /*
      Bind href to a value
    */
    function ngHrefBind() {
        return {
            restrict: 'A',
            scope: {
                ngHrefBind: '='
            },
            link: function (scope, element, attrs) {

                // set the href to the correct model value
                element.get(0).href = scope.ngHrefBind;

                // watch for any changes
                scope.$watch('ngHrefBind', function (nv, ov) {
                    // set the href to the correct model value
                    element.get(0).href = scope.ngHrefBind;
                });
            }
        };
    }



    /*
      Collapsible Panels
    */
    function collapsiblePanel() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {

                //toggle the collapse state on header click
                element.find('.panel-heading').click(toggle_collapse);

                //initialise collapse control - default is to show it
                element.find('.panel-body').addClass('collapse').addClass('in').collapse('show');

                function toggle_collapse(evt) {
                    element.find('.panel-body').collapse('toggle');
                }
            }
        };
    }


    /*
      Colour response codes
    */
    function responseCode() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {

                function updateColour() {

                    // remove all previous classes
                    element.removeClass();

                    //convert response code to an integer
                    var responseCode = +element.html();

                    if (responseCode >= 200 && responseCode < 300) element.addClass('response-200');
                    if (responseCode >= 300 && responseCode < 400) element.addClass('response-300');
                    if (responseCode >= 400 && responseCode < 500) element.addClass('response-400');
                    if (responseCode >= 500) element.addClass('response-500');
                }

                // intially set the color
                updateColour();

                // watch for any changes in the contents
                scope.$watch(function () {
                    return element.html();
                }, function (nv, ov) {
                    if (nv !== ov) updateColour();
                });
            }
        };
    }


    /*
      Initialise metis menu
    */
    sideMenu.$inject = ['$timeout', '$localize'];

    function sideMenu($timeout, $localize) {
        return {
            restrict: 'A',
            link: function (scope, element, attr) {

                $timeout(function () {

                    var current_page_urls = $localize.getPageUrls();

                    //need to know the url to know the selected page
                    var browser_base_url = window.location.href;
                    var browser_url = browser_base_url.substring((browser_base_url.indexOf(baseUrl) + baseUrl.length));

                    var selectedItem = element.find('.nav a').filter(function (idx, menuItem) {
                        var menu_url = menuItem.href.substring((menuItem.href.indexOf(baseUrl) + baseUrl.length));

                        if (browser_url === menu_url) return true;

                        // return menuItem.href === url;
                        var matching_url = false;
                        for (var url in current_page_urls) {

                            if (menu_url === '/' + current_page_urls[url]) matching_url = true;
                        }

                        return matching_url;
                    });


                    //if no matching menu item was found then stop here
                    if (!selectedItem || selectedItem.length === 0) {
                        return;
                    }

                    // if there are multiple items found use the last one
                    selectedItem = selectedItem.last();

                    // ensure the parent element is visible
                    selectedItem.parents('ul').css('display', 'block');

                    //select the menu item
                    selectedItem.parent().addClass('active');
                });
            }

        };
    }


    /*
      Top bar menu toggle
    */
    function menuToggle() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                element.click(function () {
                    $('.top-header .menu').toggleClass('expanded');
                });
            }
        };
    }

    /*
      Definition Formatter
    */
    definition.$inject = ['$swagger'];

    function definition($swagger) {
        return {
            restrict: 'E',
            scope: {
                definitionName: '='
            },
            link: function (scope, element, attrs) {
                $swagger.getDefinitions(function (json) {

                    //create schema view from definitions
                    var schemaView = new JSONSchemaView(json[scope.definitionName], 1);

                    //replace schema element with the new rendered view
                    element.replaceWith(schemaView.render());
                });
            }
        };
    }


    /*
      Simple Json Formatter
    */
    jsonFormatter.$inject = ['$swagger'];

    function jsonFormatter($swagger) {
        return {
            restrict: 'E',
            scope: {
                data: '='
            },
            link: function (scope, element, attrs) {

                //create schema view from definitions
                var schemaView = new JSONSchemaView(scope.data, 0);

                //replace schema element with the new rendered view
                element.replaceWith(schemaView.render());
            }
        };
    }


    /*
      Service to get swagger definition and resolve all references
    */
    function $swagger() {
        var vm = this;

        vm.swaggerData = null;

        //create instance of reference parser
        vm.parser = new $RefParser();

        var callbacks = [];
        var is_loading = false;

        vm.getSwaggerJson = function (callback) {

            // add to the list of callbacks we need to make
            callbacks.push(callback);


            if (swagger_url !== null) {

                // only make request once
                if (is_loading === true) return;

                is_loading = true;

                // if we have already got the data dont load it
                if (vm.swaggerData) {
                    dereference(vm.swaggerData);
                    return;
                }

                // if file is a yaml or yml file
                if (swagger_url.toLowerCase().endsWith('yaml') || swagger_url.toLowerCase().endsWith('yml')) {
                    YAML.load(swagger_url, function (json) {

                        // sotre the json so we dont need to do this again
                        vm.swaggerData = json;

                        // resolve any references
                        dereference(json);
                    });
                } else if (swagger_url.toLowerCase().endsWith('json')) {
                    $.getJSON(swagger_url, null, function (json) {
                        dereference(json);
                    });
                } else {
                    alert('File type not supported, only YAML, YML or JSON files are valid: ' + swagger_url);
                }
            } else {
                //if we havent loaded it before then get swagger json and dereference
                dereference(vm.swaggerData ? vm.swaggerData : JSON.parse(angular.element('#swagger-json').html()));
            }

            function dereference(json) {
                vm.parser.dereference(json).then(function (schema) {
                    for (var i = 0; i < callbacks.length; i++) {
                        callbacks[i].apply(vm, [schema]);
                    }
                    is_loading = false;
                    callbacks = [];
                });
            }

        };

        vm.getDefinitions = function (callback) {
            vm.getSwaggerJson(function (schema) {
                callback(schema.definitions);
            });
        };

        return vm;
    }


    /*
      Button collapse section
    */
    function collapseButton() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {
                var collapsed = true;
                var initialText = element.text();

                var regions = element.parent().siblings('[collapse-region]');
                regions.addClass('collapse').collapse('hide');

                element.click(toggle_collapse);

                function toggle_collapse() {
                    collapsed = !collapsed;
                    regions.collapse('toggle');

                    if (collapsed === true) {
                        element.text(initialText);
                    } else {
                        element.text('Close');
                    }
                }
            }
        };
    }


    /*
      Allow bootstrap tooltips on elements
    */
    function hoverTooltip() {
        return {
            restrict: 'A',
            link: function (scope, element, attrs) {

                var tooltip_position = attrs.tooltipPosition || 'top';
                var tooltip_text = attrs.hoverTooltip || '';

                element.tooltip({
                    title: tooltip_text,
                    placement: tooltip_position
                });
            }
        };
    }
})();