import re
import sys

def fix_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()

    out = []
    for i, line in enumerate(lines):
        if 'contentDescription = null,' in line and 'Icon(' not in line and 'AsyncImage(' not in line and 'Image(' not in line:
            # Check what's missing
            prefix = ""
            if 'modifier' in line and 'trailingIcon' not in lines[i-1]:
                # likely leadingIcon or something similar, let's just use Icon(Icons.Default.Star
                prefix = "Icon(androidx.compose.material.icons.Icons.Default.Star, "
            else:
                prefix = "Icon(androidx.compose.material.icons.Icons.Default.Star, "
            
            line = line.replace('contentDescription = null,', prefix + 'contentDescription = null,')
        out.append(line)

    with open(filepath, 'w') as f:
        f.writelines(out)

fix_file('app/src/main/java/com/example/ui/screens/CalculatorScreen.kt')
fix_file('app/src/main/java/com/example/ui/screens/AddProductScreen.kt')
