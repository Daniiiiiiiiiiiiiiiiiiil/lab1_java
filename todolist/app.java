package todolist;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

// класс, представляющий задачу в списке дел с атрибутами: заголовок, описание, срок выполнения, приоритет и статус.
class Task {
    static int counter = 1;
    private final int id;
    private String title;
    private String description;
    private LocalDate dueDate;
    private Priority priority;
    private Status status;

    public enum Priority { LOW, MEDIUM, HIGH }
    public enum Status { TODO, IN_PROGRESS, DONE }
    // конструктор для создания новой задачи.
    public Task(String title, String description, LocalDate dueDate, Priority priority) {
        this.id = counter++;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = Status.TODO;
    }

    // геттеры и сеттеры
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public Priority getPriority() { return priority; }
    public Status getStatus() { return status; }

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setStatus(Status status) { this.status = status; }

    // константы для цветного вывода в консоли
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RESET = "\u001B[0m";

    // возвращает ANSI код цвета в зависимости от приоритета задачи
    private String getColor(Priority priority) {
        return switch (priority) {
            case HIGH -> RED;
            case MEDIUM -> YELLOW;
            case LOW -> GREEN;
        };
    }
    // использует ANSI коды для цветного вывода в консоли
    @Override
    public String toString() {
        String color = getColor(priority);
        String date = dueDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return String.format("%sID: %d | %s | Date: %s | Priority: %s | Status: %s%s\nDescription: %s\n",
                color, id, title, date, priority, status, RESET, description);
    }
}

// класс для управления задачами: добавление, редактирование, удаление, сортировка и поиск
class TaskManager {
    private final List<Task> tasks = new ArrayList<>();
    private final String fileName = "tasks.json";
    // настройка gson
    private final Gson gson = new GsonBuilder()
            // десериализатор для LocalDate
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, ctx) ->
                    LocalDate.parse(json.getAsString()))
            // сериализатор для LocalDate
            .registerTypeAdapter(LocalDate.class, (JsonSerializer<LocalDate>) (src, typeOfSrc, ctx) ->
                    new JsonPrimitive(src.toString()))
            .setPrettyPrinting()
            .create();

    public TaskManager() {
        loadTasks();
    }

    // добавляет новую задачу в список
    public void addTask(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }
        tasks.add(task);
        System.out.println("✅ Задача успешно добавлена!\n");
        saveTasks();
    }
    // редактирует задачу по её ID
    public void editTask(int id, Scanner scanner) {
        Task task = findById(id);
        if (task == null) {
            System.out.println("❌ Задача не найдена");
            return;
        }

        System.out.print("Введите новое название (Enter — без изменений): ");
        String title = scanner.nextLine().trim();
        if (!title.isBlank()) task.setTitle(title);

        System.out.print("Введите новое описание (Enter — без изменений): ");
        String description = scanner.nextLine();
        if (!description.isBlank()) task.setDescription(description);

        System.out.print("Введите новую дату (гггг-мм-дд) или Enter: ");
        LocalDate date = inputDate(scanner);
        if (date != null) task.setDueDate(date);

        System.out.print("Введите новый приоритет (LOW, MEDIUM, HIGH) или Enter: ");
        Task.Priority priority = inputPriority(scanner);
        if (priority != null) task.setPriority(priority);

        System.out.print("Введите статус (TODO, IN_PROGRESS, DONE) или Enter: ");
        Task.Status status = inputStatus(scanner);
        if (status != null) task.setStatus(status);

        System.out.println("✅ Задача обновлена!");
        saveTasks();
    }
    // удаляет задачу по её ID
    public void deleteTask(int id) {
        Task task = findById(id);
        if (task != null) {
            tasks.remove(task);
            System.out.println("🗑️ Задача удалена");
            saveTasks();
        } else {
            System.out.println("❌ Задача не найдена");
        }
    }
    // выводит список всех задач
    public void listTasks() {
        if (tasks.isEmpty()) {
            System.out.println("📭 Список задач пуст");
            return;
        }
        tasks.forEach(System.out::println);
    }

    // выводит список просроченных задач
    public void listOverdueTasks() {
        LocalDate today = LocalDate.now();
        List<Task> overdue = tasks.stream()
                .filter(t -> t.getDueDate().isBefore(today) && t.getStatus() != Task.Status.DONE)
                .toList();
        if (overdue.isEmpty()) {
            System.out.println("✅ Просроченных задач нет");
        } else {
            System.out.println("🕗 Просроченные задачи:");
            overdue.forEach(System.out::println);
        }
    }
    // выводит список выполненных задач
    public void listDoneTasks() {
        List<Task> done = tasks.stream()
                .filter(t -> t.getStatus() == Task.Status.DONE)
                .toList();
        if (done.isEmpty()) {
            System.out.println("✅ Выполненных задач нет");
        } else {
            System.out.println("✔️ Выполненные задачи:");
            done.forEach(System.out::println);
        }
    }
    // сортирует задачи по дате выполнения
    public void sortByDate() {
        tasks.sort(Comparator.comparing(Task::getDueDate));
        System.out.println("📅 Задачи отсортированы по дате");
    }
    // сортирует задачи по приоритету
    public void sortByPriority() {
        tasks.sort(Comparator.comparing(Task::getPriority));
        System.out.println("⭐ Задачи отсортированы по приоритету");
    }
    // ищет задачи по ключевому слову в заголовке, описании, приоритете или статусе, нечуствителен к регистру
    public void search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            System.out.println("❌ Ключевое слово не может быть пустым");
            return;
        }
        List<Task> result = tasks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                             t.getDescription().toLowerCase().contains(keyword.toLowerCase()) ||
                             t.getPriority().toString().equalsIgnoreCase(keyword) ||
                             t.getStatus().toString().equalsIgnoreCase(keyword))
                .toList();
        if (result.isEmpty()) {
            System.out.println("❌ Ничего не найдено");
        } else {
            result.forEach(System.out::println);
        }
    }

    // находит задачу по её ID
    private Task findById(int id) {
        return tasks.stream().filter(t -> t.getId() == id).findFirst().orElse(null);
    }
    // сохраняет список задач в JSON-файл
    private void saveTasks() {
        try (Writer writer = new FileWriter(fileName)) {
            gson.toJson(tasks, writer);
        } catch (IOException e) {
            System.out.println("❌ Ошибка сохранения задач: " + e.getMessage());
        }
    }
    // загружает задачи из JSON-файла
    private void loadTasks() {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("⚠️  Файл задачи не найден, начинается с пустого списка");
            return;
        }
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Task>>() {}.getType();
            List<Task> loadedTasks = gson.fromJson(reader, listType);
            if (loadedTasks != null) {
                // проверка уникальности ID
                Set<Integer> ids = new HashSet<>();
                for (Task task : loadedTasks) {
                    if (!ids.add(task.getId())) {
                        throw new IllegalStateException("Найден повтор ID задачи: " + task.getId());
                    }
                }
                tasks.addAll(loadedTasks);
                if (!tasks.isEmpty()) {
                    Task last = tasks.get(tasks.size() - 1);
                    Task.counter = last.getId() + 1;
                }
            }
        } catch (JsonParseException e) {
            System.out.println("❌ Ошибка анализа файла задачи JSON: " + e.getMessage());
            System.out.println("⚠️ Начинаем с пустого списка задач");
        } catch (IOException e) {
            System.out.println("❌ Ошибка чтения файла задачи: " + e.getMessage());
            System.out.println("⚠️  Начинаем с пустого списка задач");
        } catch (IllegalStateException e) {
            System.out.println("❌ Ошибка: " + e.getMessage());
            System.out.println("⚠️  Начинем с пустого списка задач");
        }
    }

    public List<Task> getTasks() {
        return tasks;
    }

    // запрашивает непустую строку от пользователя
    public String inputNonEmptyString(Scanner scanner, String errorMessage) {
        while (true) {
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("⚠️ " + errorMessage);
            System.out.print("Try again: ");
        }
    }
    // запрашивает дату, возвращает null при пустом вводе
    public LocalDate inputDate(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim();
            if (input.isBlank()) return null;
            try {
                return LocalDate.parse(input);
            } catch (Exception e) {
                System.out.println("⚠️ Некорректный формат даты. Попробуйте снова (гггг-мм-дд): ");
            }
        }
    }
    // запрашивает приоритет, возвращает null при пустом вводе
    public Task.Priority inputPriority(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.isBlank()) return null;
            try {
                return Task.Priority.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Некорректный приоритет, введите LOW, MEDIUM, or HIGH: ");
            }
        }
    }
    // запрашивает статус, возвращает null при пустом вводе
    public Task.Status inputStatus(Scanner scanner) {
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.isBlank()) return null;
            try {
                return Task.Status.valueOf(input);
            } catch (IllegalArgumentException e) {
                System.out.println("⚠️ Неккоректный статус, введите TODO, IN_PROGRESS, or DONE: ");
            }
        }
    }
}

// класс, который реализует консольный интерфейс
public class app {
    private static final Scanner scanner = new Scanner(System.in);
    private static final TaskManager manager = new TaskManager();
    // основной метод программы, запускающий консольное меню
    public static void main(String[] args) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine();

            switch (choice) {
                case "1" -> manager.listTasks();
                case "2" -> addTask();
                case "3" -> editTask();
                case "4" -> deleteTask();
                case "5" -> manager.sortByDate();
                case "6" -> manager.sortByPriority();
                case "7" -> searchTasks();
                case "8" -> manager.listOverdueTasks();
                case "9" -> manager.listDoneTasks();
                case "0" -> {
                    System.out.println("👋 Выход из программы");
                    scanner.close();
                    return;
                }
                default -> System.out.println("⚠️ Неверный ввод, повторите попытку");
            }
        }
    }

    // выводит консольное меню
    private static void printMenu() {
        System.out.println("""
                ╔════════════════════════════════╗
                ║         TO-DO LIST MENU        ║
                ╠════════════════════════════════╣
                ║ 1. Показать все задачи         ║
                ║ 2. Добавить задачу             ║
                ║ 3. Редактировать задачу        ║
                ║ 4. Удалить задачу              ║
                ║ 5. Сортировать по дате         ║
                ║ 6. Сортировать по приоритету   ║
                ║ 7. Поиск по атрибутам          ║
                ║ 8. Список просроченных задач   ║
                ║ 9. Список выполненных задач    ║
                ║ 0. Выйти                       ║
                ╚════════════════════════════════╝
                Ваш выбор: """);
    }
    // запрашивает данные для создания новой задачи и добавляет её в список
    private static void addTask() {
        System.out.print("Введите название задачи (минимум 1 символ): ");
        String title = manager.inputNonEmptyString(scanner, "Название задачи не может быть пустым");

        System.out.print("Введите описание: ");
        String description = scanner.nextLine();

        System.out.print("Введите дату выполнения (гггг-мм-дд): ");
        LocalDate date = manager.inputDate(scanner);
        if (date == null) {
            System.out.println("⚠️ Дата не указана, используется текущая дата");
            date = LocalDate.now();
        }

        System.out.print("Введите приоритет (LOW, MEDIUM, HIGH): ");
        Task.Priority priority = manager.inputPriority(scanner);
        if (priority == null) {
            System.out.println("⚠️ Приоритет не указан, используется MEDIUM");
            priority = Task.Priority.MEDIUM;
        }

        manager.addTask(new Task(title, description, date, priority));
    }
    // запрашивает ID задачи для редактирования
    private static void editTask() {
        System.out.print("Введите ID задачи для редактирования: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            manager.editTask(id, scanner);
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Некорректный ID");
        }
    }
    // запрашивает ID задачи для удаления
    private static void deleteTask() {
        System.out.print("Введите ID задачи для удаления: ");
        try {
            int id = Integer.parseInt(scanner.nextLine());
            manager.deleteTask(id);
        } catch (NumberFormatException e) {
            System.out.println("⚠️ Некорректный ID");
        }
    }
    // запрашивает ключевое слово для поиска задач
    private static void searchTasks() {
        System.out.print("Введите ключевые слова (название, описание, приоритет или статус): ");
        String keyword = scanner.nextLine();
        manager.search(keyword);
    }
}