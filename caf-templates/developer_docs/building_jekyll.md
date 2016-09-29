# Building the Jekyll Project

*Please Note: This is not required if you are simply writing documentation. This document covers the steps required to get, modify and build the Jekyll project*.

## Prerequisites

The following items are required to build the Jekyll project:

1. Git - https://git-scm.com/downloads
2. Ruby - https://www.ruby-lang.org/
3. RubyGems - https://rubygems.org/pages/download
4. Jekyll - (installed using RubyGems)
5. Bundler - (installed using RubyGems)

#### Installing Git

First install Git which comes with an installer to guide you through the process.

#### Installing Ruby

Next we need to install Ruby. Depending on the platform you are using the installation procedure varies, however for Windows there is a RubyInstaller (http://rubyinstaller.org/).

When installing Ruby ensure you enable the option `Add Ruby executables to your PATH`.

We also need to install the Ruby Development Kit which can be found on the RubyInstaller download page as well. Download and extract it to a 'DevKit' folder in the Ruby directory. The Ruby directory is usually located in `C:\RubyXX-x64`.

Navigate to this folder in the `Command Prompt`, then we need to run the following commands:

    ruby dk.rb init  
    ruby dk.rb review  
    ruby dk.rb install  

#### Installing RubyGems

Visit the RubyGems download page and download the `ZIP` version and extract it.

Next open the `Command Prompt` and navigate the to directory you just extracted the files to. To install RubyGems we need to type the following command:

    ruby setup.rb

*Note: You may have to launch the `Command Prompt` as an Administrator*

#### Installing Jekyll

Now that RubyGems has been installed we can use it to install Jekyll. Type the following into the `Command Prompt`:

    gem install jekyll

*Note: If an error occurs stating that https://rubygems.org cannot be accessed this is likely because proxy settings have not been configured correctly. To resolved this you can set the proxies by using the following commands: (replace with the appropriate proxy and port)*

    set http_proxy=http://{{proxy}}:{{port}}  
    set https_proxy=http://{{proxy}}:{{port}}

#### Installing Bundler

Finally we need to install Bundler which is responsible for managing the dependencies required by the Jekyll project. In the `Command Prompt` type the following command:

    gem install bundle


## Getting the Jekyll project

Once all the prerequisites have been install we can then get the Jekyll project from the Git repository.

Make a folder that will contain the Jekyll project, open the `Command Prompt` (or `Git Bash` depending on the configuration chosen when installing Git) and navigate to the folder.

Next we need to clone the Git repository. Type the following commands:

    git clone https://github.hpe.com/caf/caf-documentation.git
    cd caf-jekyll

Next we need to install all the dependencies that the Jekyll project requires. This can be done automatically using Bundler which we installed earlier. To begin the installation type the following command:

    bundle install

This is the project set up and ready to use.

## Building the Jekyll project

The Jekyll project is built from the `Command Prompt`. To build the project use the following command:

    jekyll build

When modifying the Jekyll project a more useful command to use is:

    jekyll serve

This will perform an initial build, but will also watch for any changes to any files in the project and if any change it will re-build any necessary files automatically.

This command will also host the project on a local web server which can be accessed by going to the following URL:

    http://localhost:4000/caf-documentation/

## Publishing changes to the Jekyll project

There are several branches in the Git repository that each have a different purpose.

- `master` - This branch should contain the latest stable version of the project
- `development` - This branch should be used when developing new features or modifying existing features. Only once the changes are stable and tested should they be pushed the `master` branch.
- `gh-pages` - This branch contains the public version of the documentation site. Changing the contents of this branch will change the live version of the documentation site.

**Important Note**: When pushing to the `gh-pages` branch you must update the `_config.yml` file.  
By default the `baseurl` property is set to `/caf-documentation`, however as this branch contains the live site the baseurl should be changed to `https://pages.github.hpe.com/caf/caf-documentation`.
