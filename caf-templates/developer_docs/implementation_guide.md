# Documentation Implementation Guide

## Getting Started

The documentation site is a Jekyll project consisting of HPE themed layouts and all the necessary configuration to build and deploy your documentation site. 

Using these layouts will provide a consistent user experience across all services.

To incorporate the documentation into your project create a `docs` folder in your repository. This will contain all files required to build your Jekyll site.

Each service's documentation should be accessible from both the GitHub site and the site hosted in the gh-pages branch of the repository.

## Using the Templates

### Installing Node.js

Node.js is a runtime environment for executing Javascript code outside of the browser. It comes bundled with npm (node package manager) that allows additional libraries to be easily installed.

Download and run the Node.js installer (https://nodejs.org/en/download/).

**Note:** To download node packages the proxy settings need to be configured correctly. To resolve this you can set the proxies by using the following commands: (replace with the appropriate proxy and port).

	set http_proxy=http://{{proxy}}:{{port}}  
    set https_proxy=http://{{proxy}}:{{port}}

### Getting the Templates

#### Installing Bower

Bower is a package manager optimized for front end development, and can be used to download the CAF Jekyll Theme. We can install bower using NPM in the Command Prompt.

	npm install -g bower
    
#### Configuring Bower

We need to provide Bower with access to Artifactory. To do this we need to install an additional package using NPM. Enter the following into the Command Prompt:

	npm install -g bower-art-resolver
    
Next, create a .bowerrc file in the documentation folder with the following contents:

    {
        "registry":
        {
            "search": [
                "http://rh7-artifactory.svs.hpeswlab.net:8081/artifactory/api/bower/bower-virtual"
            ]
        },
        "resolvers": [
            "bower-art-resolver"
        ],
        "directory": "."
    }

You should then create a bower.json file to contain information on the project along with any dependencies bower should download for you. 
Use the following command to make bower step you through the process:

    bower init

#### Getting the Templates

Navigate to the documentation directory using the Command Prompt. We can now use Bower to download the Jekyll templates and add 
this as a dependency to our bower json file.

	bower install caf-templates --save

### Installing Jekyll and its Dependencies

To build the documentation on a local machine you will need to install Jekyll's dependencies.

##### Ruby

Download the windows installer & Ruby Development Kit (http://rubyinstaller.org/)

Run installer, ensuring that you check the `Add Ruby executables to your PATH` option

Extract Ruby Developer Kit to a 'DevKit' folder in the Ruby directory. The Ruby directory is usually located in `C:\RubyXX-x64`.

Navigate to the 'DevKit' directory in the Command Prompt, then we need to run the following commands:

```
ruby dk.rb init  
ruby dk.rb review  
ruby dk.rb install  
```

##### Installing RubyGems

RubyGems is a package manager for Ruby, allowing quick and easy installation of Ruby applications or libraries.

Download RubyGems zip file (https://rubygems.org/pages/download)

Extract zip and navigate to the directory in Command Prompt (Run as Administrator).

Install using the following command:

	ruby setup.rb

##### Installing Bundler

Bundler is an application that provides a consistent environment for Ruby projects by tracking and installing the exact gems and versions that are needed.

Simply enter the following in the Command Prompt to install Bundler:

	gem install bundle

**Note**: If an error occurs when trying to install node modules or rubygems this is likely because proxy settings have not been configured correctly. To resolve this you can set the proxies by using the following commands: (replace with the appropriate proxy and port).

	set http_proxy=http://{{proxy}}:{{port}}  
    set https_proxy=http://{{proxy}}:{{port}}


##### Installing Grunt

Grunt is a task runner that allow the automation of tasks. We can use Grunt to easily build the documentation, host it on a local webserver and deploy the documentation site.

Grunt and Grunt plugins are installed and managed via [npm](https://www.npmjs.org/), the [Node.js](https://nodejs.org/) package manager.

Once Node.js and npm are installed we can open the Command Prompt and install the Grunt CLI by using the following command:

	npm install -g grunt-cli
    
##### Configuring Bundler

To manage all our dependencies and to ensure the versions of each are compatible with each other we need to create a `Gemfile` (no file extension) in our documentation folder. This will allow us to use Bundler to get any required packages at the correct version. The Gemfile should contain the following:

    source "https://rubygems.org"

    gem 'rouge', '1.11.1'
    gem 'jekyll-coffeescript', '1.0.1'
    gem 'jekyll-watch', '1.5.0'
    gem 'jekyll-assets', '2.2.5'
    gem 'sprockets', '3.6.3'
    gem 'jekyll', '3.1.6'
    gem 'colorator', '0.1'
    gem 'jekyll-paginate', '1.1.0'

    gem 'wdm', '>= 0.1.0' if Gem.win_platform?


Using Command Prompt navigate to the documentation folder within your repository. Enter the following command:

	bundle install
    
##### Configuring NPM
    
We also need to tell NPM which packages to download for us. To do this create a package.json file in the documentation folder containing the following:

    {
      "name": "documentation",
      "version": "1.0.0",
      "description": "CAF Documentation Jekyll Project",
        "dependencies": {
          "grunt": "^1.0.1",
          "grunt-build-control": "^0.7.1",
          "grunt-jekyll": "^0.4.4"
        },
        "devDependencies": {
          "grunt-exec": "^1.0.0"
        }
    }

Change the name, version and description to values that are appropriate to your project.

Now that NPM is configured, we can enter the following into the Command Prompt to install Grunt and the Grunt Tasks for us:
    
    npm install

**Note:** This will add a `node_modules` folder to the directory. You should add it to the .gitignore file to avoid checking in unnecessary files.

### Configuring Grunt Tasks

Grunt tasks are configured in the Gruntfile.js file which can should also be added to the documentation folder with the following contents:

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
                    push: true,
                    connectCommits: false,
                    message: 'Built %sourceName% from commit %sourceCommit% on branch %sourceBranch%'
                },
                pages: {
                    options: {
                        remote: 'git@github.hpe.com:ashley-glenn-hunter/sample-docs.git',
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


To enable publishing of site automatically using Grunt you will need to enter the required information into the Gruntfile.js under the `buildcontrol` task.

More information on publishing documentation can be found [here](#publishing).

You must specify the `remote` property which should contain the url to the Git repository. The easiest method is using SSH, however if you need to specify specific login information you can provide the login name and token granting access.

### Creating The Documentation Site

To create our site we need to add a `_config.yml` file to the documentation directory. This file contains the configuration for Jekyll. Give the file the following contents:

    title: My Service
    email: caf@hpe.com
    version: 1.0.0

    description: My Service Description
    baseurl: "/documentation"

    # Set a custom logo in the navigation bar
    # navigation_image: 'assets/img/worker-framework-logo.png'

    # Provide a custom stylesheet to style you site
    # custom_stylesheet: 'assets/css/site.css'

    # Build settings
    exclude: ['node_modules']

    markdown: kramdown
    highlighter: rouge

    gems: ['jekyll-coffeescript', 'jekyll-watch', 'jekyll-assets', 'jekyll-paginate']

    layouts_dir:  caf-templates/_layouts
    includes_dir: caf-templates/_includes
    sass:
        sass_dir: caf-templates/_sass

    # paginate: 5
    # paginate_path: "/pages/en-us/blog/page:num/"

You should update the `title`, `email`, `description` properties with appropriate values.
The `baseurl` property should also be updated with the url that you site will be hosted on by GitHub pages.

#### Configuring .gitignore

You should add a `.gitignore` file to both the root repository folder to exclude the following files and folders:

-   docs/node_modules
-   docs/_site
-   docs/.sass-cache
-   docs/.jekyll-metadata
-   docs/Gemfile.lock
-   docs/caf-templates

You should also add a `.gitignore` file to the `docs` folder to exclude the following files and folders:

-   node_modules
-   _site
-   .sass-cache
-   .jekyll-metadata
-   Gemfile.lock


#### Setting Up Navigation

To provide the necessary configuration for site navigation and localization you should add an `_data` folder to the documentation folder.
This folder will contain several json files use to add links to the top navigation bar, the side navigation bar and the footer of the page.

Add the following files to the `_data` directory and update the values to reflect the pages on your site:

##### top_navigation.json

    {
        "navigation_items": [{
            "title": {
                "en-us": "Overview"
            },
            "url": {
                "en-us": "pages/en-us/overview"
            },
            "icon": "",
            "primary": false
        }, {
            "title": {
                "en-us": "Team"
            },
            "url": {
                "en-us": "pages/en-us/team"
            },
            "icon": "",
            "primary": false
        }, {
            "title": {
                "en-us": "Blog"
            },
            "url": {
                "en-us": "pages/en-us/blog/"
            },
            "icon": "",
            "primary": false
        }]
    }

##### side_navigation.json

    {
        "navigation_items": [{
            "title": {
                "en-us": "My Service"
            },
            "children": [{
                "title": {
                    "en-us": "Overview"
                },
                "url": {
                    "en-us": "pages/en-us/overview"
                }
            },
            {
                "title": {
                    "en-us": "Blog"
                },
                "url": {
                    "en-us": "pages/en-us/blog/index"
                }
            }]
        }]
    }

##### footer_links.json

    {
        "feedback_url": "https://github.hpe.com/caf/service/issues",
        "copyright": "©2017 HPE Common Application Framework",
        "footer_logo": "assets/img/footer-logo.png",
        "footer_columns": [{
            "title": {
            "en-us": "Social"
            },
            "links": [{
            "title": {
                "en-us": "GitHub"
            },
            "url": {
                "en-us": "http://github.hpe.com"
            }
            }]
        }]
    }

#### Setting up Localization

Additionally you need to add some basic support for localization. We do this by adding a `localizations` folder to the `_data` folder. 
This will contain a json file contain all supported languages and a file containing localized version of words used within the documentation site.

Add the following files to the `localizations` folder:

##### documentation_localization_static.json

    {
        "Accept": {
            "en-us": "Accept"
        },
        "Access Token": {
            "en-us": "Access Token"
        },
        "API Key": {
            "en-us": "API Key"
        },
        "API Key Authentication": {
            "en-us": "API Key Authentication"
        },
        "Authenticate": {
            "en-us": "Authenticate"
        },
        "Authorization URL": {
            "en-us": "Authorization URL"
        },
        "Cancel": {
            "en-us": "Cancel"
        },
        "Close": {
            "en-us": "Close"
        },
        "Code": {
            "en-us": "Code"
        },
        "Choose A Region": {
            "en-us": "Choose A Region"
        },
        "Contact information": {
            "en-us": "Contact information"
        },
        "Content-Type": {
            "en-us": "Content-Type"
        },
        "Contribute": {
            "en-us": "Contribute"
        },
        "Description": {
            "en-us": "Description"
        },
        "Details": {
            "en-us": "Details"
        },
        "Examples": {
            "en-us": "Examples"
        },
        "false": {
            "en-us": "false"
        },
        "Filter by tags": {
            "en-us": "Filter by tags"
        },
        "Flow": {
            "en-us": "Flow"
        },
        "Header Preview": {
            "en-us": "Header Preview"
        },
        "Headers": {
            "en-us": "Headers"
        },
        "HTTP Basic Authentication": {
            "en-us": "HTTP Basic Authentication"
        },
        "In": {
            "en-us": "In"
        },
        "License": {
            "en-us": "License"
        },
        "Location": {
            "en-us": "Location"
        },
        "Models": {
            "en-us": "Models"
        },
        "Name": {
            "en-us": "Name"
        },
        "OAuth 2.0 Authentication": {
            "en-us": "OAuth 2.0 Authentication"
        },
        "Parameters": {
            "en-us": "Parameters"
        },
        "Password": {
            "en-us": "Password"
        },
        "Paths": {
            "en-us": "Paths"
        },
        "Please follow OAuth flow, copy access token from OAuth and paste it here.": {
            "en-us": "Please follow OAuth flow, copy access token from OAuth and paste it here."
        },
        "Request": {
            "en-us": "Request"
        },
        "Required": {
            "en-us": "Required"
        },
        "Responses": {
            "en-us": "Responses"
        },
        "Schema": {
            "en-us": "Schema"
        },
        "Scheme": {
            "en-us": "Scheme"
        },
        "Scopes": {
            "en-us": "Scopes"
        },
        "Security": {
            "en-us": "Security"
        },
        "Security Schema": {
            "en-us": "Security Schema"
        },
        "Send Request": {
            "en-us": "Send Request"
        },
        "Submit Feedback": {
            "en-us": "Submit Feedback"
        },
        "Summary": {
            "en-us": "Summary"
        },
        "Terms of service": {
            "en-us": "Terms of service"
        },
        "Terms of Service": {
            "en-us": "Terms of Service"
        },
        "Token URL": {
            "en-us": "Token URL"
        },
        "true": {
            "en-us": "true"
        },
        "Try this operation": {
            "en-us": "Try this operation"
        },
        "User Name": {
            "en-us": "User Name"
        },
        "Version": {
            "en-us": "Version"
        },
        "Warning: Deprecated": {
            "en-us": "Warning: Deprecated"
        }
    }

##### languages.json

    {
        "en-us": {
            "country": "United States",
            "language": "English",
            "continent": "North America"
        }
    }

#### Creating your Home Page

You should begin by adding an `index.md` or `index.html` file in the documentation folder as a landing page for your site. 
See [here](#landing) for more information on how to use the landing page layout.

You should create an `assets` subdirectory in the documentation folder to contain any additional media files you might want to include in your site.

### Writing Documentation

Documentation should be written in either markdown format or HTML within a `pages` -> `en-us` folder. 

Documentation written using markdown is accessible to people browsing the source code via GitHub and can also be included into your documentation site. Use markdown except when the content is truly HTML, avoid HTML in markdown files.

To add links to a page either in the top navigation bar or the side navigation bar, update the appropriate json file in the `caf-templates` -> `_data` folder. 
When changing any json files in this directory, stop the grunt task and run `jekyll build`. After this is complete start the grunt task again.

Each markdown file should specify how it should be presented by adding a layout attribute at the top of the page. Eg:

```
---
layout: default
---

Page content goes here.
```

Additionally, you should grant the documentation team access so they can make changes directly and submit pull requests. This can be achieved by going to the Settings area of your repository on GitHub. Under the Collaborators section you can grant access to users by entering their username, full name or email address.

**Note:** If you are migrating from the old documentation site you can simply add your existing markdown files to the `pages` -> `en-us` folder to provide the content for the site.

### Testing Documentation

Testing the documentation site before publishing will help identify any errors. The Command Prompt output when running on a local machine is more helpful than the error messages GitHub provides when building the documentation fails.

To there are two available grunt tasks for testing documentation on a local machine, `build` and `serve`.

The `build` task will build the Jekyll project.

The `serve` task will build the Jekyll project, host it on a local webserver. This task will also watch for any changes to files and rebuild when necessary.

To run a task, type `grunt` followed by the task name you want to run in the Command Prompt.

There is an addition grunt task to update the template files to the latest version. This can be run by entering the following in the Command Prompt:

    grunt update

### Publishing Documentation
<a name="publishing"/>

For each release of your service the latest documentation should be published to gh-pages making it accessible to visitors of your documentation web site. 

During the release process `grunt publish` will update the template files to the latest version, build your documentation and push it into your repositories gh-pages branch. Before publishing new documentation your build job must preserve the previous version of the documentation in a folder within gh-pages named vMajor.Minor.Patch, where Major Minor and Patch are placed with the previous versions numbers.

GitHub will automatically assign your documentation site a pages.github.hpe.com/caf/repository-name domain once a gh-pages branch is created.

Before publishing documentation ensure that the site has been configured correctly. A configuration file called `_config.yml` will need to have the `baseurl` property set correctly. It should be set to the url your GitHub pages site will be located at, eg:

	https://pages.github.hpe.com/caf/elements

**Note**: There should be no trailing '/' in the url.

Documentation can then be published to the `gh-pages` branch using the `grunt publish` task. 

**Note**: Git must be installed and available to use from the Command Prompt for the `grunt publish` task to work.

The publishing of documentation should be performed as part of the release process of your service. Engage the release engineering team for assistance in this build configuration.

## Provided Layouts

### Landing Page Layout
<a name="landing"/>

When you wish to implement a landing for your GitHub page, you can make use of the `landing` layout provided. 
The landing page should be located in an index.html or index.md file in the root documentation folder.

The landing page had a header section that has a background image, logo, title, slogan and a button. 
The next section contains an overview as well as a short description of the tent pole features.
The third section contains some images of the product in use.
The final section provides links to social sites such as Twitter and GitHub.

To set these attributes, we simply have to set the layout to `landing` and define the other required attributes in the YAML Frontmatter for that page.

Add any images you want to use to the `assets` directory you created in your documentation folder. 
Update the paths used in the YAML below to point to the correct locations.

The icons for features are from the [Elements Icon Set](https://pages.github.hpe.com/caf/elements/pages/en-us/css.html#icons), 
however if you wish to use an image instead simply replace the `icon` property with an `image` property and se the value to the url of the image.

This can be seen below:  
```yaml
---
layout: landing

logo: assets/img/caflogo.png
background_image: assets/img/landing_4.jpg
title: Common Application Framework <br><small>From the Information Management and Governance Research and Development Team at Hewlett Packard Enterprise</small>
slogan: The Microservices based solution to your Big Data Analytics problems the <br>Common Application Framework accelerates time to value.
button:
    title: Learn More
    url: pages/en-us/what_is_caf

features:
    title: About CAF
    subtitle: Common Application Framework is a platform for building next generation cloud based applications. It provides Identity Management, Storage, Auditing, Job Services, Data Processing and Document Classification services as well as frameworks for creating new services. Developed using Microservices architecture, the Framework utilizes fault-tolerant queues for messaging, Docker containers for packaging and rapid deployment as well as Apache Mesos with Marathon for orchestration and massive scalability.
    feature_list:
        - icon: hpe-expand
          title: Scalable
          subtitle: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        - icon: hpe-cube
          title: Containerized
          subtitle: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        - icon: hpe-cloud
          title: Cloud Based
          subtitle: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        - icon: hpe-shield-configure
          title: Fault Tolerant
          subtitle: Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.

showcase:
    title: Made with CAF
    subtitle: CAF is being used to develop next generation enterprise applications.
    left_showcase: 
    	img: 'assets/img/showcase/showcase-2.png'
        url: ''
    center_showcase: 
    	img: 'assets/img/showcase/showcase-1.png'
        url: ''
    right_showcase: 
    	img: 'assets/img/showcase/showcase-3.png'
        url: ''

social:
    title: Get Involved
    subtitle: Get involved in the CAF project. Suggest new features, report issues or take part in development.
    social_list:
        - icon: hpe-social-twitter
          title: Twitter
          subtitle: Follow us on Twitter to keep up with the latest news and updates from the CAF team or to get in touch with us!
          link:
            title: '@twitterhandle'
            url: http://twitter.com
        - icon: hpe-social-github
          title: GitHub
          subtitle: Fork CAF services on GitHub and submit a pull request to help contribute to the project! Or if you have discovered an issue, report it to us.
          link:
            title: Fork CAF
            url: https://github.hpe.com/caf
        - icon: hpe-chat
          title: Blog
          subtitle: Follow our blog to find out all of the exciting news and announcements regarding CAF.
          link:
            title: Read More Here
            url: pages/en-us/blog/index.html
---
```

You should create a Twitter account for your service. We recommend creating a gmail or yahoo email account for the Twitter login credentials so they can be shared with your team allowing anyone to add content. Ensure to update the social links on this page to point to the correct locations.

The above YAML would result in the following:

![Alt text](images/landing_example.PNG)

---------------------------------------

### API Landing Page Layout

The `api-landing` layout can be used to provide several links to the most important sections of the documentation available at a glance.

The layout is comprised of a header, an optional description or overview and finally several description panels that provide links to other parts of the documentation.
Each panel should contain an icon, a header, a description and a url.

To set these attributes, we simply have to set the layout to `api-landing` and define the other required attributes in the YAML Frontmatter for that page.

This can be seen below:  

```yaml
---
layout: api-landing
title: Getting Started

header: Api & Docs
description: From raw HTTP to helper libraries, get up and running quickly in your language of choice.
links:
    - icon: hpe-alarm
      title: Quickstarts
      description: A set of simple tutorials with code snippets on how to use CAF Audit.
      url: quickstarts
    - icon: hpe-task
      title: Tutorials
      description: Sample applications that cover common use cases in a variety of languages. Download, test drive, and tweet them yourself.
      url: tutorials
    - icon: hpe-code
      title: API Reference
      description: In-depth reference docs for the REST APIs provided by CAF Audit
      url: api
    - icon: hpe-catalog
      title: Helper Libraries
      description: Interact with CAF Audit APIs in Java, Node.js, Ruby, Python, PHP, C#, Javascript and more.
      url: helper-libs
---
```

The above YAML would result in the following:

![Alt text](images/api-landing_example.PNG)

---------------------------------------

### Default Layout

The `default` layout can be used for pages that contain content solely obtained from a markdown file. For example any pages that simply contain text, images, code samples etc.. should all use the default layout.

To use the default layout add the following to the top of the markdown page:

```yaml
---
layout: default
title: Page Title Here
last_updated: Created and last modified by John Smith on May 1, 2016
---
```
The `title` attribute allows you to specify the page title, and the `last_updated` attribute (optional) allows you to identify when the page was created/last modified.

An example of how a page using this layout might look:

![Alt text](images/default_layout.PNG)


#### Adding a Banner

Provide quick access to the most important links at the top of pages by adding a banner. 

```yaml
---
layout: default
title: Getting Started

banner:
    icon: 'assets/img/hard-hat.png'
    title: Worker Framework
    subtitle: Analyze a Larger Range of Formats
    links:
        - title: GitHub
          url: https://github.hpe.com/caf/worker-framework
---
```

Which will look like the following:

![Alt text](images/banner.png)

---------------------------------------

### Blog Layout

The `blog` layout can be used to create a *blog* page for your service. This can be useful to provide information about new releases, upcoming versions or other useful information.

To use the `blog` layout add the following to the top of your HTML file. The blog page should be located in `pages` -> `en-us` -> `blog` -> `index.html`

```yaml
---
layout: blog
no_posts_message: No Blog Posts
---
```

The `no_posts_message` property allows you to define the message shown when no posts exist. If this is not specified 'No Blog Posts' will be shown.

Add a `_posts` folder to the documentation folder which should contain the markdown files for each blog post. Blog posts should be named using the following format to ensure posts are ordered correctly: YEAR-MONTH-DAY-Blog_Title.md

Posts should have the following attributes at the top of the markdown file:

```
---
title: Title here
author: John Smith
datestamp: May 1st 2016 - 10:50am
---
```

You will also need to active the pagination settings in `_config.yml` as they are currently commented out:

    paginate: 5
    paginate_path: "/pages/en-us/blog/page:num/"

![Alt text](images/blog_layout.png)

---------------------------------------

### Team Layout

The `team` layout can be used to show the developers currently working on a particular service. 
It can also be used to provide contact details for each member and links to their social sites such as Twitter, GitHub etc..

To use the `team` layout add the following to the top of your markdown file:

```yaml
---
layout: team

header: The Elements Team

team_members:
    - name: Gita Narasimhan
      email: gita.narasimhan@hpe.com
      avatar: /assets/img/user.png
      social:
        - title: Email
          icon: hpe-social-email
          url: mailto:gita.narasimhan@hpe.com

    - name: Alastair Payne
      email: alastair.stu.payne@hpe.com
      avatar: /assets/img/user.png
      social:
        - title: Email
          icon: hpe-social-email
          url: mailto:alastair.stu.payne@hpe.com

    - name: Ashley Hunter
      email: ashley-glenn.hunter@hpe.com
      avatar: /assets/img/user.png
      social:
        - title: Twitter
          icon: hpe-social-twitter
          url: https://twitter.com/ashh640
        - title: GitHub
          icon: hpe-social-github
          url: https://github.com/ashh640
        - title: Email
          icon: hpe-social-email
          url: mailto:ashley-glenn.hunter@hpe.com
---
```

An example of how a page using this layout might look:

![Alt text](images/team_layout.png)

---------------------------------------

### Swagger Layout

The `swagger` layout can be used to present APIs in a user friendly way. Swagger definitions can be provided in either `YAML` or `JSON`. The swagger definition should follow the specification [found here](http://swagger.io/specification/).

To use the `swagger` layout set the layout attribute to `swagger`. An addition title attribute can be set to specify the page title.

##### Load YAML/JSON file

The easiest way to display your API is to load a YAML or JSON file containing your Swagger schema from your repository.

To do this simply add the `swagger_url` attribute to the YAML Frontmatter and set it's value to the relative path to the .yaml, .yml or .json file.

And example of this might look like this:

```yaml
---
layout: swagger
title: API
swagger_url: swagger.yaml
---
```

**Note:** The url provided must point to a file on the same domain, this cannot be a file loaded from an external site due to browser security restrictions. To allow loading from a different domain you must ensure that the server allows requests from the GitHub pages domain.

##### Alternate Method - Embedded YAML

An alternative method of specifying a Swagger schema is to simply add the YAML directly to your markdown file within the YAML frontmatter region. This is not recommended as the yaml would be duplicated and could become out of sync.

Simply add the Swagger YAML below the layout attribute. This can be seen below:

```yaml
---
layout: swagger
title: API

# Swagger YAML goes here
swagger: "2.0"
info:
  title: "CAF Job Service"
  version: "1.0"
  description: |
    Allows background operations to be sent to the CAF Workers, and for the
    operations to be tracked and controlled.
basePath: /job-service/v1
tags:
  - name: Jobs
    description: Job Control
# ....
---
```

##### Alternate Method - Embedded JSON

A Swagger schema in `JSON` format can also be embedded directly into the markdown file. This is not recommended as the json would be duplicated and could become out of sync.

The slight difference when using `JSON` is you must specify the `swagger_json` attribute and add the `JSON` as the value of this attribute.

An example can be seen below:

```yaml
---
title: API Json
layout: swagger

# Swagger JSON goes here
swagger_json: {
    "swagger": "2.0",
    "info": {
        "title": "CAF Job Service",
        "version": "1.0",
        "description": "Allows background operations to be sent to the CAF Workers, and for the\noperations to be tracked and controlled."
    },
    "basePath": "/job-service/v1",
    "tags": [
        {
            "name": "Jobs",
            "description": "Job Control"
        }
    ],
    # ....
  }
---
```

#### Swagger 'Try' Functionality

The Swagger layout provides the option to allow a user to try out an API within the browser. The layout will create a form that contains all of the fields for the parameters required and will allow the user to input some sample values and view the response.

The form enforces validation based on whether or not a field is required and also the data type of the field to ensure data is in the correct format.

This functionality is disabled by default. To enabled it simply add the following on to the top of the YAML frontmatter:

```yaml
---
title: API
layout: swagger
swagger_try_enabled: true

swagger: 2.0
# ....
---
```

**Note:** A deployed version of your service must be available to use the Swagger Try functionality. Note also that the server the hosts the service must accept cross origin requests from this the GitHub Pages site. For more information  [read here](https://github.com/swagger-api/swagger-editor/blob/master/docs/cors.md).

Below is an example of how a complete Swagger layout would look like:

![Alt text](images/swagger_example.PNG)


## Customizing The Site

Each service may have a specific logo associated with it. To make it appear in the top navigation bar add the following property to the `_config.yml` and giving it a value of the image url:

```yaml
navigation_image: 'assets/img/worker-framework-logo.png'
```

Further customization can be achieved by adding a custom stylesheet to override some of the default styles. 
Add the following property to the `_config.yml` file with the url of the stylesheet you wish to use:

```yaml
custom_stylesheet: 'site.css'
```


## Adding Showcase Entry

A showcase site for CAF components can be found at [cafapi.github.io](http://cafapi.github.io) and provides a brief description of each service along with a link to each service's documentation site.

To add your service to the showcase page:

- Fork the [https://github.com/CAFapi/CAFapi.github.io](https://github.com/CAFapi/CAFapi.github.io) repository.
- Add a new section with an overview of your service:
	+ Add a new markdown file under `showcase` -> `en-us` -> `services`
	+ Update the showcase.json file found in the `_data` folder
- Schedule review of showcase entry and service documentation with Frank. This review should be run from the fork and show the finished showcase entry and service docs.
- Build your changes using the "grunt" task.
- Submit pull request for Frank to review.
- Frank will approve or reject content. If approved he will merge the changes.
