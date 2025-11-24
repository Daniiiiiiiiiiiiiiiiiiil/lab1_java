#!/bin/bash
# скрипт для компиляции и запуска

cd "$(dirname "$0")"
echo "компиляция"
javac -cp ".:/Users/a1/Documents/2 course/java/lab1/lib/gson-2.10.1.jar" todolist/*.java
if [ $? -eq 0 ]; then
    echo "✔️ успешно"
    echo "запуск"
    java -cp ".:/Users/a1/Documents/2 course/java/lab1/lib/gson-2.10.1.jar" todolist.app
else
    echo "✖️ ошибка"
fi
