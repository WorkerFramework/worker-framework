/*
  Swagger Controller
*/
(function() {

    // register controllers and directives
    angular.module('caf').directive('restColour', restColour);
    angular.module('caf').filter('restTypes', restTypes);
    angular.module('caf').controller('SwaggerCtrl', SwaggerCtrl);
    angular.module('caf').directive('schemaValue', schemaValue);

    SwaggerCtrl.$inject = ['$scope', '$swagger'];

    function SwaggerCtrl($scope, $swagger) {
        var vm = this;

        // show progress spinner when parsing
        vm.loading = true;

        // store some global properties
        vm.protocols = [];
        vm.consumes = [];
        vm.produces = [];

        vm.schema = null;

        // store whether or not swagger try functionality should be enabled - default is false
        vm.swagger_try_enabled = swagger_try_enabled || false;

        // add filtering by tag capabilities
        vm.selected_tags = [];

        vm.toggle_tag = function(tag) {
            // if tag is currently selected then remove it
            if (vm.tag_selected(tag)) {
                // get the index of the tag
                var tag_index = vm.selected_tags.indexOf(tag);

                // splice array to remove tag
                vm.selected_tags.splice(tag_index, 1);
            } else {
                // if tag isnt selected simply add it to the list
                vm.selected_tags.push(tag);
            }
        };

        vm.tag_selected = function(tag) {
            return vm.selected_tags.indexOf(tag) !== -1;
        };

        vm.panel_visible = function(tags) {

            // if no tags are selected then it will always be visible
            if (vm.selected_tags.length === 0) return true;

            // otherwise if any of the tags are selected then show it
            var matching_tags = tags.filter(function(tag) {
                return vm.selected_tags.indexOf(tag) !== -1;
            });

            // if the panel has any selected tags show it, otherwise hide it
            return matching_tags.length > 0;
        };

        // resolve all references
        $swagger.getSwaggerJson(function(schema) {

            // update the values from the schema
            vm.protocols = schema.schemes || [location.protocol.substring(0, location.protocol.length - 1)];
            vm.consumes = schema.consumes || [];
            vm.produces = schema.produces || [];

            // store the entire schema
            vm.schema = schema;

            // hide the spinner
            vm.loading = false;

            // update the UI
            $scope.$digest();
        });
    }

    /*
      Set panel colour based on rest type
    */


  function restColour() {
        return {
            restrict: 'A',
            scope: {
                restColour: '='
            },
            link: function(scope, element, attrs) {
                switch (scope.restColour.toLowerCase()) {
                    case 'get':
                        element.addClass('panel-info');
                        break;

                    case 'post':
                        element.addClass('panel-success');
                        break;

                    case 'put':
                        element.addClass('panel-warning');
                        break;

                    case 'delete':
                        element.addClass('panel-danger');
                        break;

                    case 'options':
                    case 'head':
                    case 'patch':
                        element.addClass('panel-success');
                        break;
                }
            }
        };
    }


    /*
      Filter to get only rest types
    */


    function restTypes() {
        return function(prop_list) {

            var output = {};
            var valid_rest_types = ['get', 'put', 'post', 'delete', 'options', 'head', 'patch'];

            //iterate each property and only return valid rest type properties
            for (var prop in prop_list) {
                var is_valid = valid_rest_types.indexOf(prop) !== -1;

                if (is_valid) output[prop] = prop_list[prop];
            }

            return output;
        };
    }


    function schemaValue() {
        return {
            restrict: 'A',
            require: ['ngModel', '^apiProperties'],
            scope: {
                schemaValue: '='
            },
            link: function(scope, element, attrs, ctrls) {

                // get controllers
                var ng_model_controller = ctrls[0];
                var api_properties = ctrls[1];

                scope.$watch(function() {
                    return ng_model_controller.$modelValue;
                }, function(nv, ov) {
                    // if the value is undefined or the same as the previous value then do nothing
                    if (!nv || nv === ov) return;

                    // update the values in the parent
                    api_properties.try_parameters[scope.schemaValue] = nv;
                });
            }
        };
    }

})();
