module.exports = function(grunt) {

    grunt.initConfig({
        jekyll: {
            build: {
                options: {
                    serve: false,
                    incremental: false,
                    watch: false,
                    config: '_config.yml',
                    bundleExec: true
                }
            },
            serve: {
                options: {
                    serve: true,
                    incremental: false,
                    watch: true,
                    baseurl: '/documentation',
                    config: '_config.yml',
                    open_url: true,
                    bundleExec: true
                }
            }
        },
        exec: {
            bower_install: 'bower install',
            bower_uninstall: 'bower uninstall caf-templates',
            bower_clean: 'bower cache clean'
        },
        buildcontrol: {
            options: {
                dir: '.',
                commit: true,
				connectCommits: false,
                push: true,
                message: 'Built %sourceName% from commit %sourceCommit% on branch %sourceBranch%'
            },
            pages: {
                options: {
                    remote: 'git@github.com:WorkerFramework/worker-framework.git',
                    login: '',
                    token: '',
                    branch: 'gh-pages'
                }
            }
        }
    });

    grunt.loadNpmTasks('grunt-build-control');
    grunt.loadNpmTasks('grunt-jekyll');
    grunt.loadNpmTasks('grunt-exec');

    grunt.registerTask('default', ['jekyll:build']);

    grunt.registerTask('build', ['jekyll:build']);
    grunt.registerTask('serve', ['jekyll:serve']);
    grunt.registerTask('update', ['exec:bower_uninstall', 'exec:bower_clean', 'exec:bower_install']);

    grunt.registerTask('publish', ['exec:bower_uninstall', 'exec:bower_clean', 'exec:bower_install', 'buildcontrol:pages']);
};
