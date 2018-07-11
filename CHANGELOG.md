# Changelog

All notable changes to Net2Plan will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) and this project adheres to [Semantic Versioning](http://semver.org/).



## [0.6.0.1] - 2018-07-11 

### Fixed
	- Some bug fixes.

####

## [0.6.0] - 2018-07-05
### Added
	- Possibility to attach monitoring traces of carried traffic (series of pairs date-traffic) to the links, and also traces of offered traffic to demands and multicast demands. Methods to synthetically create, import and export from Excel, and manipulate those traces are included in the GUI. 
	- Basic forecasting functionalities, also accessible from the GUI, to predict traffic in links and demands from the monitoring traces. 
	- Basic traffic matrix estimation functionalities, also accessible from the GUI, to predict demands and multicast demands traffic, from link occupations. 
	- QoS functionalities. Demands and multicast demands can be tagged with a user-defined QoS. Then, the user can define scheduling policies, that assign link bandwidth to the traversing demands and multicast demands with user-defined priorities, according to their QoS.
	- Resources can now we dettached from the nodes, and attached later to other nodes.
	- Nodes now can have different (X,Y) positions at different user-defined layouts.
	- Links can now be coupled to demands in the same layer, as long as no coupling cycles occur. 
	- Demands can now be of the type source routing or hop-by-hop routing. Previously, all the demands in the same layer where forced to be of the same type.
	- Multicast trees can now be trees that reach just a subset of the egress nodes of the associated demand.
	- Shared risk groups can now also be dynamic, meaning that the associated links and nodes are defined by a function of the network state, not statically set.
	- Internally, most of the methods work with SortedSets instead of Set, so the ordering when iterating those sets is platform independent (in its turn, the order in Set is not defined, and can change from one from one program execution to the next). 
	- GUI: Possibility to navigate through the tables, e.g. picking the origin node of a link in the links table, picks that node in the nodes table. It is possible to move back and forward the picked elements.
	- GUI: Easier selection of the current active layer in a left panel.
	- GUI: The tabbed panel at the right, now shows ALL the layers information, not just the active layer.
    
### Fixed
	- Some bug fixes.

####



## [0.5.3] - 2018-02-14
### Added
    - Added Optical Fiber Utils.
	- Report for Optical Fiber networks following calculations of Gaussian  Noise Model.
    
### Fixed
    - Site mode does not break the tool when loading a new topology.
	- Corrected layer table tags.
	- Sorting table columns works as it should.
	- Other bug fixes.

####


## [0.5.2] - 2017-05-24
### Added
    - Added a new tab under the "Demands" table for traffic matrix visualization and control.
    - Added a new visualization option for giving color to links based on their current utilization.
    
### Changed
    - Reworked multi-layer control panel to work as a non-intrusive pop-up.
    - Separated canvas display from table display: Pick option.
    - Changed project license to BSD 2-clause.
    
### Fixed
    - Focus panel refreshes correctly again.
    - Returned non-n2p loading modules: SNDLib...
    - Can now load designs with no population attribute.
    - Giving order to a table now works as intended.
    - Canvas no longer draws layers in the inverse order.

####

## [0.5.1-Beta.1] - 2017-04-20
### Added
    - Added capacity for multiple line selection on tables.
    - Exporting function for writing tables on an Excel file.
    - Added a helper under the options window for looking for solvers under the PC.
    - New development tools.
### Changed
    - Improved performance for large scale algorithms.
    - Tools now ask for confirmation when changing between them.
    - Tables' window can now run all keyboard shortcuts.
### Fixed
    - Solved a problem on the focus panel where it crashed when an element on its list was removed.
    - Solved right-click pop-up options order issues.
    - What-If analysis now correctly shows the help box.
    - "View/Edit backup routes" now displays properly.
    - Solved a problem where forwarding rules were not displayed on the focus panel.
    