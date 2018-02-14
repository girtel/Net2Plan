# Changelog

All notable changes to Net2Plan will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/) and this project adheres to [Semantic Versioning](http://semver.org/).


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
    