#!/usr/bin/env bash
# Build giao diện vào gui/build/GameTournamentGUI.jar rồi mở chương trình.
# Chạy từ thư mục GameTournament để dùng chung dữ liệu data/gtms.dat với bản console.
set -e
cd "$(dirname "$0")/.."

# Windows (Git Bash) ngăn cách đường dẫn bằng ';', macOS/Linux bằng ':'
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=";" ;;
    *) SEP=":" ;;
esac

rm -rf gui/out
javac -encoding UTF-8 -nowarn -d gui/out -sourcepath "src${SEP}gui/src" gui/src/gtms/gui/GuiApp.java

# Công cụ jar nằm trong thư mục bin của JDK, thường không có sẵn trong PATH
JAR=$(command -v jar || true)
if [ -z "$JAR" ]; then
    JHOME=$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.home = //p' | tr -d '\r')
    if command -v cygpath >/dev/null; then JHOME=$(cygpath -u "$JHOME"); fi
    JAR="$JHOME/bin/jar"
fi

mkdir -p gui/build
if "$JAR" --create --file gui/build/GameTournamentGUI.jar --main-class gtms.gui.GuiApp -C gui/out .; then
    echo "Đã build gui/build/GameTournamentGUI.jar"
    java -jar gui/build/GameTournamentGUI.jar
else
    echo "Không tạo được file jar, chạy trực tiếp từ gui/out"
    java -cp gui/out gtms.gui.GuiApp
fi
