<p align="center">
    <img src="https://raw.githubusercontent.com/girtel/Net2Plan/develop/Net2Plan-GUI/Net2Plan-GUI-Plugins/Net2Plan-NetworkDesign/src/main/resources/resources/common/net2plan-logo.jpg" height="300">
</p>
<p align="center">
    <img src="https://travis-ci.org/girtel/Net2Plan.svg?branch=master">
    <img src="https://s-a.github.io/license/img/bsd-2-clause.svg">
</p>

# Introduction
Net2Plan is a Java developed tool for the planning, optimization and evaluation of communication networks.

For further information, please feel free to follow the next web pages:
* The [Net2Plan website](http://net2plan.com).
* The [Net2Plan user's guide](http://net2plan.com/documentation/current/help/usersGuide.pdf).
* The [Net2Plan API Javadoc](http://net2plan.com/documentation/current/javadoc/api/index.html).

# About Net2Plan
Net2Plan is a free and open-source Java tool devoted to the planning, optimization and evaluation of communication networks. It was originally thought as a tool to assist on the teaching of communication networks courses. Eventually, it got converted into a powerful network optimization and planning tool for both the academia and the industry, together with a growing repository of network planning resources.

Net2Plan is built on top of an abstract network representation, so-called network plan, based on abstract components: nodes, links, traffic unicast and multicast demands, routes, multicast trees, shared-risk groups, resources and network layers. The network representation is technology-agnostic, thus Net2Plan can be adapted for planning networks of any technology. Technology-specific information can be introduced in the network representation via user-defined attributes attached to any of the abstract components mentioned above. Some attribute names has been fixed to ease the adaptation of well-known technologies (e.g. IP networks, WDM networks, NFV scenarios).

Net2Plan is implemented as a Java library along with both command-line and graphical user interfaces (CLI and GUI, respectively). The GUI is specially useful for laboratory sessions as an educational resource, or for a visual inspection of the network. In its turn, the command-line interface is specifically devoted to in-depth research studies, making use of batch processing or large-scale simulation features. Therefore, Net2Plan is a tool intended for a broad spectrum of users: industry, research, and academia.

# Building instructions
Since Net2Plan 0.4.1, the project is being built through the use of Maven.

The Maven command to build the project is the following:

`clean package`

The result is a _Net2Plan-VERSION.zip_ package containing the resulting program. By default, this file can be found under the _target_ folder of the _Net2Plan-Assembly_ module.

# Developer tools
Net2Plan offers an alternative method of running the program without the need of going through the packaging phase. This is achieved by running the main method under:

`com.net2plan.launcher.GUILauncher`

Which can be found at:

`Net2Plan -> Net2Plan-Launcher`

Note that this is meant for running the graphical interface of the project.

# Learning materials
New users may want to checkout the Net2Plan's [Youtube channel](https://www.youtube.com/channel/UCCgkr1wlMlO221yhFGmWZUg), alongside the [user's guide](http://net2plan.com/documentation/current/help/usersGuide.pdf), for an introduction into the basics of the provided tools. 

# License
Net2Plan is licensed under the [Simplified BSD License](https://opensource.org/licenses/BSD-2-Clause). Meaning that it is completely free and open-source. As a consequence of this, you may use parts of Net2Plan or the complete package inside your own programs for free, can make money from them and do not have to disclose your code. Although, you are obliged to mention that you are using Net2Plan in your program.
