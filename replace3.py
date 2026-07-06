import re

file_path = r"C:\Users\rajib\Desktop\vidplay\app\src\main\java\com\helpofai\videoplayer\feature\library\HomeScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

pattern = re.compile(r"if \(selectedTab == 0\) \{.*?\} else if \(selectedTab == 1\) \{", re.DOTALL)

replacement = """if (selectedTab == 0) {
                    com.helpofai.videoplayer.feature.library.components.LibraryHomeTab(
                        state = state,
                        isTablet = isTablet,
                        onVideoClick = onVideoClick,
                        onFavoriteClick = onFavoriteClick,
                        onRenameClick = { videoToRename = it },
                        onDeleteClick = { videoToDelete = it },
                        onShareClick = onShareClick,
                        onNavigateToPlaylists = {
                            selectedTab = 2
                            selectedFolder = it
                        }
                    )
                } else if (selectedTab == 1) {"""

new_content = pattern.sub(replacement, content)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(new_content)

print("Home tab replaced.")
