import re

file_path = r"C:\Users\rajib\Desktop\vidplay\app\src\main\java\com\helpofai\videoplayer\feature\library\HomeScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

pattern = re.compile(r"\} else if \(selectedTab == 1\) \{.*?\} else if \(selectedTab == 2\) \{", re.DOTALL)

replacement = """} else if (selectedTab == 1) {
                    com.helpofai.videoplayer.feature.library.components.LibraryFoldersTab(
                        state = state,
                        selectedFolder = selectedFolder,
                        isTablet = isTablet,
                        onFolderClick = { selectedFolder = it },
                        onViewModeChange = { viewModel.updateFolderViewMode(it) },
                        onVideoClick = onVideoClick,
                        onFavoriteClick = onFavoriteClick,
                        onRenameClick = { videoToRename = it },
                        onDeleteClick = { videoToDelete = it },
                        onShareClick = onShareClick
                    )
                } else if (selectedTab == 2) {"""

new_content = pattern.sub(replacement, content)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(new_content)

print("Folders tab replaced.")
