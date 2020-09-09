# S-Price Calculator - List of Changes
#v1.2.3
### New Features
- Added a button for resetting read settings without deleting shop or product data
### Other Changes
- Column selection components in read settings definition now only offer valid options 
(Eg. only number columns for price)
- Changed button ordering in main menu

#v1.2.2
### New Features
- Added a new view that is displayed while the search field is empty
- Added a new view that is shown when there are no search results
- Added a Google button for googling the product
- Product details view now automatically opens when the search returns only a single product
### Other Changes
- Changed product name
- Added a new logo
- Altered secondary color in the color scheme
- Changed column ordering in search results view

#v1.2.1
### New Features
- Added a new button for comparing shop prices
- Added clear history feature to menu
- Sale count information is now optional when reading product input data
### Bugfixes
- Refactored shop deletion feature so that it won't get stuck anymore

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