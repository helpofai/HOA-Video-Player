import sys

file_path = r"C:\Users\rajib\Desktop\vidplay\app\src\main\java\com\helpofai\videoplayer\feature\library\HomeScreen.kt"

with open(file_path, "r", encoding="utf-8") as f:
    lines = f.readlines()

# selectedTab == 2 block starts at line 577 (index 576): "} else if (selectedTab == 2) {"
# The block ends at line 952 (index 951): "}" (matching the else if)

new_lines = lines[:577]
new_lines.append("                    com.helpofai.videoplayer.feature.library.components.LibraryPlaylistsTab(\n")
new_lines.append("                        state = state,\n")
new_lines.append("                        selectedFolder = selectedFolder,\n")
new_lines.append("                        isTablet = isTablet,\n")
new_lines.append("                        onPlaylistClick = { selectedFolder = it },\n")
new_lines.append("                        onVideoClick = onVideoClick,\n")
new_lines.append("                        onFavoriteClick = onFavoriteClick,\n")
new_lines.append("                        onRenameClick = { videoToRename = it },\n")
new_lines.append("                        onDeleteClick = { videoToDelete = it },\n")
new_lines.append("                        onShareClick = onShareClick\n")
new_lines.append("                    )\n")
new_lines.extend(lines[952:])

with open(file_path, "w", encoding="utf-8") as f:
    f.writelines(new_lines)
print("File updated successfully.")
