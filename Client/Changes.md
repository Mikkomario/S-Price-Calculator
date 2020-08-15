# S-Price Calculator - List of Changes
## v1.2
### Major Changes
- Completely refactored internal data handling. All data needs to be re-imported. 
- "Luettavat tiedostot" -folder now only contains files to import while already processed files are moved to 
"luetut tiedostot" folder
- List of displayed products is initially empty, until the user inputs a search word
### New Features
- Added support for .csv files
- Shop and product data can now be removed directly from the UI
- Each product row now also displays the amount of savings gained from that purchase
- File import view now warns about overly large files that may cause memory problems
- Errors are now properly logged into "log" folder
### Bugfixes
- Loading views now display properly

## v1.1.2
### New Features
- Added loading views to display lengthy background process statuses
### Fixes
- Products without sales group ids wouldn't load when reading from local json storage - fixed