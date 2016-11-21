# Introduction
Net2Plan is a Java developed tool for the planning, optimization and evaluation of communication networks.

For further information, please feel free to follow the next web pages:
* The [Net2Plan website](www.net2plan.com).
* The [Net2Plan 0.4.1 user's guide](http://net2plan.com/documentation/current/help/usersGuide.pdf).
* The [Net2Plan 0.4.1 API Javadoc](http://net2plan.com/documentation/current/javadoc/api/index.html).

#About Net2Plan
Net2Plan is a free and open-source Java tool devoted to the planning, optimization and evaluation of communication networks. It was originally thought as a tool to assist on the teaching of communication networks courses. Eventually, it got converted into a powerful network optimization and planning tool for both the academia and the industry, together with a growing repository of network planning resources.

Net2Plan is built on top of an abstract network representation, so-called network plan, based on abstract components: nodes, links, traffic unicast and multicast demands, routes, protection segments, multicast trees, shared-risk groups and network layers. The network representation is technology-agnostic, thus Net2Plan can be adapted for planning networks of any technology. Technology-specific information can be introduced in the network representation via user-defined attributes attached to any of the abstract components mentioned above. Some attribute names has been fixed to ease the adaptation of well-known technologies (i.e. IP networks).

Net2Plan is implemented as a Java library along with both command-line and graphical user interfaces (CLI and GUI, respectively). The GUI is specially useful for laboratory sessions as an educational resource, or for a visual inspection of the network. In its turn, the command-line interface is specifically devoted to in-depth research studies, making use of batch processing or large-scale simulation features. Therefore, Net2Plan is a tool intended for a broad spectrum of users: industry, research, and academia.

#Building instructions
Since Net2Plan 0.4.1, the project is being built through the use of Maven. For this task, two different Maven profiles are provided:
* _build-without-javadoc_: Quick build where the Javadoc build step is ignored. Useful for checking that everything in the program is right before creating a final release. 
* _build-with-javadoc_: The main profile of the project. The Javadoc and the documentation will be added alongside the result that the former profile produced.

For both profiles, the Maven command-line to execute is the one as follows:

`clean package -P PROFILE_NAME`

The result of this command is a _net2plan-VERSION.zip_ file containing the resulting program. The default location for this file is the "target" folder inside the project's structure.

Final releases of Net2Plan can be also be found at the project's [website](http://net2plan.com/download.php).

#Running instructions
Net2Plan offers an alternative way of compiling the program in order to avoid the use of Maven during development. This is achieved by running the main class found in:

`com.net2plan.launcher.GUILauncher`

For this class to work, it is necessary to add the libraries found at `src\main\resources\lib-essentials` to the project's build-path. More information about these libraries [here](http://net2plan.com/license.php).

Note that this method only works for launching the GUI version of the project.

#Learning materials
Net2Plan's [Youtube channel](https://www.youtube.com/channel/UCCgkr1wlMlO221yhFGmWZUg) alongside the [user's guide](http://net2plan.com/documentation/current/help/usersGuide.pdf) provide a nice start for new users to start learning the multiple tools that Net2Plan provides. 

#License
Net2Plan is licensed under the [GNU Lesser General Public License Version 3](http://www.gnu.org/licenses/lgpl.html) and onwards (called the _LGPL_). Meaning that it is completely free and open-source. As a consequence of this, you may use parts of Net2Plan or the complete package inside your own programs for free, can make money from them and do not have to disclose your code. Although, you are obliged to mention that you are using Net2Plan in your program.