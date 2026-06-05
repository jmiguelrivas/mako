# Changelog

## Backlog
- Add gesture section
- Add option for home double tap
- Add option for home long press (currently shows settings)
- Move clock tap option to gestures
- Move date tap option to gestures
- Add option for clock long press
- Add option for date long press
- Add option for sleep among the apps that can be selected for gestures
- Add option for open settings among the apps that can be selected for gestures

## 42

- Add click on icon open app settings checkbox
- Add different ways to add admin access to mako to be able to turn off screen

## 41

- Screen opacity control
- Fix navigation bar color + opacity
- Add catppuccin latte light theme
- Prevent screen rotation checkbox
- Add clouds wallpaper
- Add mountains wallpaper
- Add pond wallpaper
- Add double tap to put phone to sleep
- Add date tap action
- Replace context menu with menu bar
- Add multiple selection option

## 40

- Add zoom control
- Prevent non letter for been the initial of the profiles

## 39

- Add Themes Attributions
- Clean and organized the project

## 38

- Add Rama Theme
- Add Catppuccin Mocha Theme
- Add Dracula Theme
- Add Mélange Dark Theme
- Add Tokyo Night Theme
- Fix Keyboard Navigation
- Integrate background options with the theme section
- Add Custom Font Option
- Add Custom Color Picker

## 37

- Fix missing font on radio group for dialog_group_delete
- Fix missing scroll area for dialog_group_delete and dialog_group_add
- Add default new group when deleting a group

## 36

- Add PIN protection for settings
- Reorganize settings
- Fix issue where the search bar was not reset when opening apps
- Reformat German (DE) translations
- Add Changelog

## 35

- Separate flavors (no special features yet)
- Add German (DE) translations
- Add Turkish (TR) translations
- Update license from GPLv3 to GPLv3-or-later
- Add language switcher
- Add profile indicator visibility toggle
- Add fullscreen toggle

## 34

- Add wallpaper support for the home screen
- Add always-visible search bar toggle

## 33

- Split settings into multiple files to improve collaboration
- Add branding documentation
- Add attribution documentation
- Replace Jersey 25 TrueType font with OTF version
- Add negative/monochrome logo variant
- Add navigation between documents
- Fix back navigation issue that caused app reload
- Improve search performance
- Add collapsible sections in settings
- Add custom icon selection

## 32

- Show app version on the About page

## 31

- Add work profile support
- Refactor preferences storage
- Fix issue where checkboxes reset when navigating away from Settings
- Fix "Default" group name not updating consistently
- Ensure consistent group order between main view and Settings
- Fix issue where deleting a group removed it twice from storage

## 30

- Rewrite preferences data structure for improved robustness and consistency
- Use fixed-width Base36 group IDs (minimum 11 characters)
- Assign deterministic IDs for system groups
- Generate user group IDs from creation timestamp encoded in Base36
- Improve default initialization. `initPrefs()` now creates a complete and consistent preferences
  state on first run
