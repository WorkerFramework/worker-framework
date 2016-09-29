(function() {

    angular.module('caf').service('$localize', localizeService);

    angular.module('caf').directive('localizeButton', localizeButton);
    angular.module('caf').directive('localizeText', localizeText);
    angular.module('caf').directive('localizeAll', localizeAll);
    angular.module('caf').controller('FooterCtrl', FooterCtrl);
    angular.module('caf').controller('TopNavCtrl', TopNavCtrl);
    angular.module('caf').controller('SideNavCtrl', SideNavCtrl);

    angular.module('caf').controller('LocalizeModalCtrl', LocalizeModalCtrl);

    function localizeService() {
        var vm = this;

        //Parse the JSON files
        vm.languages = JSON.parse(document.getElementById('supported-languages').innerHTML);
        vm.static_documentation = JSON.parse(document.getElementById('documentation-localization').innerHTML);
        vm.current_page_urls = JSON.parse(document.getElementById('current-page-urls').innerHTML);
        vm.json_urls = null;

        //Set the default language then get the currently selected language (saved by browser) OR set to the default language
        vm.default_language = 'en-us';
        vm.selected_language = localStorage.getItem("selected-language") || vm.default_language;

        vm.getSelectedLanguage = function() {

            var selected_language = {
                abbreviation: vm.selected_language,
                language: vm.languages[vm.selected_language]
            };

            return selected_language;
        };

        vm.getLanguages = function() {
            return vm.languages;
        };

        vm.getDefaultLanguage = function() {
            return vm.default_language;
        };

        vm.getDocumentationStatic = function() {
            return vm.static_documentation;
        };

        vm.getPropertyCaseInsensitive = function(localize_object, language_abbreviation) {
            for (var prop in localize_object) {
                if (prop.toLowerCase() === language_abbreviation.toLowerCase()) return localize_object[prop];
            }

            return localize_object[vm.default_language];
        };

        // saves the selected language in local storage
        vm.setSelectedLanguage = function(language) {
            localStorage.setItem("selected-language", language);
            vm.selected_language = language;
        };

        // store the possible urls the current page can have
        vm.setJsonUrls = function(urls) {
            vm.json_urls = urls;
        };

        // return the urls either from either the JSON or the YAML frontmatter
        vm.getPageUrls = function() {
            return vm.json_urls || vm.current_page_urls;
        };

        // navigates to the correct page when the language is changed
        vm.goToLocalizedUrl = function() {
            var page_urls = vm.getPageUrls();
            if (!page_urls) return;

            var translated_url = page_urls[vm.selected_language] || page_urls[vm.default_language];

            if (translated_url !== window.location.href.split(baseUrl + '/').pop()) {
                window.location.href = baseUrl + '/' + translated_url;
            }
        };

        return vm;
    }


    localizeButton.$inject = ['$localize', '$rootScope'];

    function localizeButton($localize, $rootScope) {
        return {
            restrict: 'E',
            template: '<div class="dropdown">' +
                '<button class="btn inline-dropdown dropdown-toggle" type="button" data-toggle="modal" data-target="#language-modal" aria-haspopup="true" aria-expanded="false">' +
                '	<i class="hpe-icon hpe-language" aria-hidden="true"></i>' +
                '	<span>{{ loc.current_language }} ({{ loc.current_abbreviation | uppercase }})</span>' +
                '</button>' +
                '</div>',
            controller: function() {
                var vm = this;

                // gets all the possible languages to be displayed
                vm.languages = $localize.getLanguages();

                //Get current selected language
                var selected_language = $localize.getSelectedLanguage();

                // ensure the selected language exists
                if(!selected_language.language || !selected_language.language.country) {

                    // set the selected language to the default if something has went wrong
                    $localize.setSelectedLanguage($localize.getDefaultLanguage());

                    // now get the selected language
                    selected_language = $localize.getSelectedLanguage();
                }

                vm.current_abbreviation = selected_language.abbreviation;
                vm.current_language = selected_language.language.country;

                $rootScope.$on('language-change', function() {

                    // update the selected language
                    var selected_language = $localize.getSelectedLanguage();
                    vm.current_abbreviation = selected_language.abbreviation;
                    vm.current_language = selected_language.language.country;
                });
            },
            controllerAs: 'loc'
        };
    }

    LocalizeModalCtrl.$inject = ['$localize', '$rootScope'];

    function LocalizeModalCtrl($localize, $rootScope) {
        var vm = this;

        vm.languages = $localize.getLanguages();

        vm.selected_language = $localize.getSelectedLanguage().abbreviation;

        vm.continents = {};

        // iterate each language - extract its continent and the countries associated with each continent
        for (var abbreviation in vm.languages) {
            var language_data = vm.languages[abbreviation];

            // check if the continent has already been defined
            if (!vm.continents.hasOwnProperty(language_data.continent)) {
                vm.continents[language_data.continent] = {
                    countries: []
                };
            }

            // add the country, abbreviation and the language to the continent data
            vm.continents[language_data.continent].countries.push({
                abbreviation: abbreviation,
                country: language_data.country,
                language: language_data.language
            });
        }

        $rootScope.$on('language-change', function() {
            vm.selected_language = $localize.getSelectedLanguage().abbreviation;
        });

        vm.select_region = function(region) {
            // store the new selected language
            $localize.setSelectedLanguage(region);

            // update any localized text
            $rootScope.$broadcast('language-change');

            // hide modal
            angular.element('#language-modal').modal('hide');

            $localize.goToLocalizedUrl();
        };

    }


    localizeText.$inject = ['$localize', '$rootScope'];

    function localizeText($localize, $rootScope) {
        return {
            restrict: 'A',
            scope: {
                localizeText: '='
            },
            link: function(scope, element, attrs) {

                // get documentation for the text (different languages)
                var documentation = $localize.getDocumentationStatic();
                var localized_documentation = documentation[scope.localizeText];

                // if language/translation not found the set the text to be the attribute value then return.
                if (localized_documentation === undefined) {
                    element.html(scope.localizeText);
                    return;
                }

                // perform initial localization
                localize();

                // listen for language changes
                $rootScope.$on('language-change', localize);

                // retrieves the translated text for the current language
                function localize() {

                    var abbreviation = $localize.getSelectedLanguage().abbreviation;

                    var translated_string = $localize.getPropertyCaseInsensitive(localized_documentation, abbreviation);

                    element.html(translated_string);

                }

            }
        };
    }

    function FooterCtrl() {
        var vm = this;

        vm.footer_data = JSON.parse(document.getElementById('footer-localization').innerHTML);
    }

    function TopNavCtrl($localize, $rootScope) {
        var vm = this;

        // set the url for the HPE logo
        vm.logo_url = baseUrl + '/index.html';

        vm.top_nav_data = JSON.parse(document.getElementById('top-nav-localization').innerHTML);
    }

    function SideNavCtrl($localize, $rootScope) {
        var vm = this;

        vm.side_nav_data = JSON.parse(document.getElementById('side-nav-localization').innerHTML);
    }

    localizeAll.$inject = ['$localize', '$rootScope'];

    function localizeAll($localize, $rootScope) {
        return {
            restrict: 'A',
            scope: {
                localizeAll: '='
            },
            link: function(scope, element, attrs) {

                // get all the values from the object
                var localized_titles = scope.localizeAll.title;

                var localized_urls = null;
                if (scope.localizeAll.url !== undefined)
                    localized_urls = scope.localizeAll.url;

                var icon = null;
                if (scope.localizeAll.icon !== undefined)
                    icon = scope.localizeAll.icon;

                var primary = false;
                if (scope.localizeAll.primary !== undefined)
                    primary = scope.localizeAll.primary;

                // perform initial localization
                localize();

                // listen for language changes
                $rootScope.$on('language-change', localize);

                function localize() {
                    var abbreviation = $localize.getSelectedLanguage().abbreviation;

                    // get localized strings
                    var translated_title = $localize.getPropertyCaseInsensitive(localized_titles, abbreviation);

                    // if URLs have been set, get the correct one for the current language
                    var translated_url = null;
                    if (localized_urls !== null) {
                        translated_url = $localize.getPropertyCaseInsensitive(localized_urls, abbreviation);
                        // check each URL the item can have and if any match actual page url, store the urls
                        for (var url in localized_urls) {
                            //if (localized_urls[urll] !== "" && window.location.href.indexOf(localized_urls[urll]) > -1)
                            if (localized_urls[url] !== "" && window.location.href.split(baseUrl + '/').pop() === localized_urls[url])
                                $localize.setJsonUrls(localized_urls);
                        }
                    }

                    // if icon has been set for header elements, add it inline beside title
                    if ((icon !== "" || icon === null) && element.parents('.top-header').length)
                        translated_title = '<img class="icon" src="' + baseUrl + '/' + icon + '"/> ' + translated_title;

                    if (primary)
                        element.addClass('primary');

                    // update the link text
                    element.html(translated_title);

                    // check if url is relative path or external
                    if (translated_url !== null && translated_url.indexOf('http://') !== 0 && translated_url.indexOf('https://') !== 0) {
                        translated_url = baseUrl + '/' + translated_url;
                    }

                    // update the link href
                    element.attr('href', translated_url);
                }
            }

        };
    }


})();
