package com.auca.main;

import com.auca.abstracts.AcademicMember;
import com.auca.abstracts.Person;
import com.auca.administrator.Administrator;
import com.auca.assessment.Assessment;
import com.auca.bookloan.BookLoan;
import com.auca.classroom.Classroom;
import com.auca.course.Course;
import com.auca.department.Department;
import com.auca.enrollment.Enrollment;
import com.auca.exceptions.BookUnavailableException;
import com.auca.exceptions.DuplicateRecordException;
import com.auca.exceptions.PrerequisiteMissingException;
import com.auca.exceptions.RecordNotFoundException;
import com.auca.exceptions.ValidationException;
import com.auca.feepayment.FeePayment;
import com.auca.interfaces.Identifiable;
import com.auca.interfaces.Registrable;
import com.auca.librarybook.LibraryBook;
import com.auca.student.Student;
import com.auca.teacher.Teacher;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Main {
    private static final Scanner in = new Scanner(System.in);

    // In-memory storage (typed collections)
    private static final List<Department> departments = new ArrayList<>();
    private static final List<Student> students = new ArrayList<>();
    private static final List<Teacher> teachers = new ArrayList<>();
    private static final List<Administrator> administrators = new ArrayList<>();
    private static final List<Course> courses = new ArrayList<>();
    private static final List<Classroom> classrooms = new ArrayList<>();
    private static final List<Enrollment> enrollments = new ArrayList<>();
    private static final List<Assessment> assessments = new ArrayList<>();
    private static final List<FeePayment> payments = new ArrayList<>();
    private static final List<LibraryBook> books = new ArrayList<>();
    private static final List<BookLoan> loans = new ArrayList<>();

    
    private static final Object CANCEL = new Object();
    private static final Object BACK = new Object();

    private interface Parser { Object parse(String text); }      
    private interface Picker { Object pick(); }                 
    private interface Saver { Object save(); }                   
    private interface Action { void apply(Object value); }       

    private static class Field {
        final int step;          
        final String label;
        final String hint;       
        final Parser parser;
        final Picker picker;
        Object value;
        boolean derived;         
        Action apply;            
        Field(int step, String label, String hint, Parser parser, Picker picker) {
            this.step = step; this.label = label; this.hint = hint; this.parser = parser; this.picker = picker;
        }
    }

    private static Field text(int step, String label, String hint, Parser p) { return new Field(step, label, hint, p, null); }
    private static Field pick(int step, String label, String hint, Picker p) { return new Field(step, label, hint, null, p); }
    private static Field derived(int step, String label, Object value) {
        Field f = new Field(step, label, "derived", null, null);
        f.value = value;
        f.derived = true;
        return f;
    }

    
    private static String str(Field[] f, int i) { return (String) f[i].value; }
    private static LocalDate date(Field[] f, int i) { return (LocalDate) f[i].value; }
    private static int num(Field[] f, int i) { return (Integer) f[i].value; }
    private static double dbl(Field[] f, int i) { return (Double) f[i].value; }
    private static boolean flag(Field[] f, int i) { return (Boolean) f[i].value; }
    private static BigDecimal money(Field[] f, int i) { return (BigDecimal) f[i].value; }

    public static void main(String[] args) {
        System.out.println("Welcome to AUCA_ID School Management System");
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Select an option: ");
            String input = line().trim();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("! '" + input + "' is not a number. Please type a number from the menu.");
                continue;
            }
            try {
                switch (choice) {
                    case 1 -> runRegistration(Main::registerDepartment);
                    case 2 -> runRegistration(Main::registerStudent);
                    case 3 -> runRegistration(Main::registerTeacher);
                    case 4 -> runRegistration(Main::registerAdministrator);
                    case 5 -> runRegistration(Main::registerCourse);
                    case 6 -> runRegistration(Main::registerClassroom);
                    case 7 -> runRegistration(Main::enrollStudent);
                    case 8 -> runRegistration(Main::recordAssessment);
                    case 9 -> runRegistration(Main::recordFeePayment);
                    case 10 -> runRegistration(Main::registerBook);
                    case 11 -> runRegistration(Main::borrowBook);
                    case 12 -> returnBook();
                    case 13 -> viewAllRecords();
                    case 14 -> searchById();
                    case 15 -> updateRecord();
                    case 16 -> studentSummary();
                    case 17 -> paymentHistory();
                    case 18 -> showAvailableBooks();
                    case 19 -> demonstratePolymorphism();
                    case 0 -> { System.out.println("Goodbye!"); running = false; }
                    default -> System.out.println("! Invalid option. Choose a number from 0 to 19.");
                }
            } catch (RecordNotFoundException | PrerequisiteMissingException | BookUnavailableException
                     | ValidationException | DuplicateRecordException e) {
                System.out.println("! " + e.getMessage());
            } catch (RuntimeException e) {
                // safety net: never crash because of one failed action
                System.out.println("! Unexpected problem (" + e.getMessage() + "). Returning to the main menu.");
            }
        }
    }

    private static void printMenu() {
        String[] m = {"Exit", "Register Department", "Register Student", "Register Teacher", "Register Administrator",
                "Register Course", "Register Classroom", "Enroll Student", "Record Assessment", "Record Fee Payment",
                "Register Library Book", "Borrow Library Book", "Return Library Book", "View All Records",
                "Search Record by ID", "Update Existing Record", "Student Academic Summary",
                "Student Payment History", "View Available Books", "Demonstrate Polymorphism"};
        System.out.println();
        System.out.println("============== AUCA_ID SCHOOL MANAGEMENT ==============");
        for (int i = 1; i <= 10; i++) {
            int right = (i == 10) ? 0 : i + 10;
            System.out.printf("%2d. %-28s %2d. %s%n", i, m[i], right, m[right]);
        }
    }

    private static String line() {
        if (!in.hasNextLine()) {
            System.out.println("\nInput closed. Exiting.");
            System.exit(0);
        }
        return in.nextLine();
    }

    private static boolean confirm(String question) {
        while (true) {
            System.out.print(question + " (Y/N): ");
            String s = line().trim();
            if (s.equalsIgnoreCase("Y") || s.equalsIgnoreCase("YES")) return true;
            if (s.equalsIgnoreCase("N") || s.equalsIgnoreCase("NO")) return false;
            System.out.println("! Please answer Y or N.");
        }
    }

    private static void runRegistration(Supplier<Object> form) {
        while (true) {
            Object saved = form.get();
            if (saved == null) return; // cancelled
            while (true) {
                System.out.println("  [1] Register another  [2] View record  [0] Main menu");
                System.out.print("Select option: ");
                String c = line().trim();
                if (c.equals("1")) break;
                if (c.equals("2")) ((Registrable) saved).printDetails();
                else if (c.equals("0")) return;
                else System.out.println("! Choose 1, 2 or 0.");
            }
        }
    }

  
    private static void banner(String title, String[] steps) {
        String bar = "+------------------------------------------------------------+";
        System.out.println();
        System.out.println(bar);
        System.out.println("| AUCA_ID / SCHOOL MANAGEMENT");
        System.out.println("| " + title + "  (" + steps.length + " steps)");
        System.out.println(bar);
        for (int i = 0; i < steps.length; i++) System.out.println("| [" + (i + 1) + "] " + steps[i]);
        System.out.println("| Type BACK to go to the previous field; CANCEL to discard.");
        System.out.println(bar);
    }

    
    private static Object runForm(String title, String[] steps, Field[] f, Saver saver) {
        banner(title, steps);
        int i = 0;
        int shownStep = 0;
        while (i < f.length) {
            if (f[i].derived) { i++; continue; }
            if (f[i].step != shownStep) {
                shownStep = f[i].step;
                System.out.println("\n[STEP " + shownStep + "/" + steps.length + "] " + steps[shownStep - 1].toUpperCase());
            }
            Object r = ask(f[i], i + 1);
            if (r == CANCEL) {
                if (confirm("Discard this unsaved record?")) {
                    System.out.println("Cancelled. Nothing was saved.");
                    return null;
                }
                continue;
            }
            if (r == BACK) {
                i = Math.max(0, i - 1);
                while (i > 0 && f[i].derived) i--;
                continue;
            }
            f[i].value = r;
            i++;
        }
        return review(title, steps, f, saver);
    }

    private static Object ask(Field fld, int number) {
        if (fld.picker != null) {
            System.out.printf("%02d %s [%s]%n", number, fld.label, fld.hint);
            return fld.picker.pick();
        }
        while (true) {
            System.out.printf("%02d %-20s [%s] : ", number, fld.label, fld.hint);
            String input = line().trim();
            if (input.equalsIgnoreCase("CANCEL")) return CANCEL;
            if (input.equalsIgnoreCase("BACK")) return BACK;
            try {
                return fld.parser.parse(input);
            } catch (ValidationException | DuplicateRecordException e) {
                System.out.println("! " + e.getMessage());
                System.out.println("  Please re-enter field " + String.format("%02d", number) + ":");
            }
        }
    }

    private static Object review(String title, String[] steps, Field[] f, Saver saver) {
        while (true) {
            String bar = "+------------------------------------------------------------+";
            System.out.println();
            System.out.println(bar);
            System.out.println("| REVIEW - " + title);
            System.out.println(bar);
            int shown = 0;
            for (int i = 0; i < f.length; i++) {
                if (f[i].step != shown) {
                    shown = f[i].step;
                    System.out.println("| " + steps[shown - 1].toUpperCase());
                }
                System.out.printf("|  %02d %-20s: %s%s%n", i + 1, f[i].label, show(f[i].value), f[i].derived ? "  (derived)" : "");
            }
            System.out.println(bar);
            System.out.println("| [1] Save   [2] Edit field   [0] Cancel");
            System.out.print("Select option: ");
            String c = line().trim();
            switch (c) {
                case "1" -> {
                    try {
                        return saver.save();
                    } catch (ValidationException | DuplicateRecordException | PrerequisiteMissingException
                             | BookUnavailableException | RecordNotFoundException e) {
                        System.out.println("! Cannot save: " + e.getMessage());
                        System.out.println("  Use option 2 to correct the data.");
                    } catch (RuntimeException e) {
                        System.out.println("! Unexpected error while saving (" + e.getMessage() + "). Nothing was saved.");
                    }
                }
                case "2" -> editField(f);
                case "0" -> {
                    if (confirm("Discard this unsaved record?")) {
                        System.out.println("Cancelled. Nothing was saved.");
                        return null;
                    }
                }
                default -> System.out.println("! Invalid option. Choose 1, 2 or 0.");
            }
        }
    }

    private static void editField(Field[] f) {
        System.out.print("Field number to edit (01-" + String.format("%02d", f.length) + "): ");
        int n;
        try {
            n = Integer.parseInt(line().trim());
        } catch (NumberFormatException e) {
            System.out.println("! Please type a field number.");
            return;
        }
        if (n < 1 || n > f.length) { System.out.println("! No such field."); return; }
        Field x = f[n - 1];
        if (x.derived) { System.out.println("! That field is derived and cannot be edited."); return; }
        System.out.println("Old value: " + show(x.value));
        Object r = ask(x, n);
        if (r == CANCEL || r == BACK) { System.out.println("Edit abandoned. Old value kept."); return; }
        x.value = r;
        System.out.println("Field " + String.format("%02d", n) + " updated.");
    }

    
    private static String show(Object v) {
        if (v == null) return "(none)";
        if (v instanceof Boolean b) return b ? "YES" : "NO";
        if (v instanceof Identifiable r) return "[" + r.getId() + "] " + r.getDisplayName();
        return v.toString();
    }

    
    private static <T extends Identifiable> Object pickRecord(String name, Supplier<List<T>> source,
                                                              Runnable registerNew, boolean optional) {
        while (true) {
            List<T> list = source.get();
            System.out.println("+----------------- CHOOSE " + name.toUpperCase() + " -----------------+");
            if (list.isEmpty()) System.out.println("| No " + name + " found yet. Register one now (option 1) or cancel (0).");
            for (int k = 0; k < list.size(); k++) {
                System.out.println("| " + (k + 1) + " [" + list.get(k).getId() + "] " + list.get(k).getDisplayName());
            }
            int newNo = list.size() + 1;
            System.out.println("| " + newNo + " [+] Register a new " + name);
            if (optional) System.out.println("| S Skip (assign later)");
            System.out.println("| 0 Cancel");
            System.out.print("Select option: ");
            String s = line().trim();
            if (s.equals("0") || s.equalsIgnoreCase("CANCEL")) return CANCEL;
            if (s.equalsIgnoreCase("BACK")) return BACK;
            if (optional && s.equalsIgnoreCase("S")) return null;
            try {
                int n = Integer.parseInt(s);
                if (n >= 1 && n <= list.size()) return list.get(n - 1);
                if (n == newNo) {
                    registerNew.run();   
                    continue;
                }
                System.out.println("! Choose a number between 0 and " + newNo + ".");
            } catch (NumberFormatException e) {
                System.out.println("! Please type a number from the list.");
            }
        }
    }

  
    private static Parser textP(String label, int min, int max) {
        return s -> Registrable.requireText(s, label, min, max);
    }

    private static Parser uniqueP(String label, int min, int max, Predicate<String> used) {
        return s -> {
            String v = Registrable.requireText(s, label, min, max);
            if (used.test(v)) throw new DuplicateRecordException(label + " " + v + " already exists.");
            return v;
        };
    }

    private static Parser emailP(String label) { return s -> Registrable.requireEmail(s, label); }

    private static Parser uniqueEmailP(String label) {
        return s -> {
            String v = Registrable.requireEmail(s, label);
            if (emailUsed(v)) throw new DuplicateRecordException(label + " " + v + " already exists.");
            return v;
        };
    }

    private static Parser phoneP(String label) { return s -> Registrable.requirePhone(s, label); }

    private static Parser dateP(String label, boolean mustBePast, boolean notFuture) {
        return s -> {
            LocalDate d = Registrable.parseDate(s, label);   // DateTimeParseException handled inside
            if (mustBePast) Registrable.requirePast(d, label);
            if (notFuture) Registrable.requireNotFuture(d, label);
            return d;
        };
    }

    private static Parser intP(String label, int min, int max) {
        return s -> {
            try {
                int v = Integer.parseInt(s.trim());
                if (v < min || v > max) throw new ValidationException(label + " must be between " + min + " and " + max + ".");
                return v;
            } catch (NumberFormatException e) {
                throw new ValidationException(label + " must be a whole number.");
            }
        };
    }

    private static Parser moneyP(String label) {
        return s -> {
            try {
                BigDecimal v = new BigDecimal(s.trim()).setScale(2, RoundingMode.HALF_UP);
                if (v.signum() <= 0) throw new ValidationException(label + " must be greater than zero.");
                return v;
            } catch (NumberFormatException e) {
                throw new ValidationException(label + " must be a valid amount, e.g. 150000.50");
            }
        };
    }

    private static String choiceHint(List<String> options) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            if (i > 0) sb.append(" / ");
            sb.append(i + 1).append(' ').append(options.get(i));
        }
        return sb.toString();
    }

    private static Parser choiceP(String label, List<String> options) {
        return s -> {
            String t = s.trim();
            for (String o : options) if (o.equalsIgnoreCase(t)) return o;
            try {
                int n = Integer.parseInt(t);
                if (n >= 1 && n <= options.size()) return options.get(n - 1);
            } catch (NumberFormatException e) {
                throw new ValidationException(label + " must be one of: " + choiceHint(options));
            }
            throw new ValidationException(label + " must be one of: " + choiceHint(options));
        };
    }

    private static Parser yesNoP(String label) {
        return s -> {
            String t = s.trim();
            if (t.equals("1") || t.equalsIgnoreCase("YES") || t.equalsIgnoreCase("Y")) return Boolean.TRUE;
            if (t.equals("2") || t.equalsIgnoreCase("NO") || t.equalsIgnoreCase("N")) return Boolean.FALSE;
            throw new ValidationException(label + " must be 1 (YES) or 2 (NO).");
        };
    }


    private static List<Person> allPeople() {
        List<Person> all = new ArrayList<>();
        all.addAll(students);
        all.addAll(teachers);
        all.addAll(administrators);
        return all;
    }

    private static List<Identifiable> allRecords() {
        List<Identifiable> all = new ArrayList<>();
        all.addAll(departments);
        all.addAll(students);
        all.addAll(teachers);
        all.addAll(administrators);
        all.addAll(courses);
        all.addAll(classrooms);
        all.addAll(enrollments);
        all.addAll(assessments);
        all.addAll(payments);
        all.addAll(books);
        all.addAll(loans);
        return all;
    }

    private static boolean idUsed(String id) {
        for (Identifiable r : allRecords()) if (r.getId().equalsIgnoreCase(id)) return true;
        return false;
    }

    private static boolean emailUsed(String e) {
        for (Person p : allPeople()) {
            if (p.getEmail().equalsIgnoreCase(e)) return true;
            if (p instanceof AcademicMember m && m.getInstitutionalEmail().equalsIgnoreCase(e)) return true;
        }
        return false;
    }

    private static boolean memberNumberUsed(String n) {
        for (Student s : students) if (s.getMemberNumber().equalsIgnoreCase(n)) return true;
        for (Teacher t : teachers) if (t.getMemberNumber().equalsIgnoreCase(n)) return true;
        return false;
    }

    private static boolean studentNumberUsed(String n) {
        for (Student s : students) if (s.getStudentNumber().equalsIgnoreCase(n)) return true;
        return false;
    }

    private static boolean employeeNumberUsed(String n) {
        for (Teacher t : teachers) if (t.getEmployeeNumber().equalsIgnoreCase(n)) return true;
        return false;
    }

    private static boolean staffNumberUsed(String n) {
        for (Administrator a : administrators) if (a.getStaffNumber().equalsIgnoreCase(n)) return true;
        return false;
    }

    private static boolean deptCodeUsed(String c) {
        for (Department d : departments) if (d.getDepartmentCode().equalsIgnoreCase(c)) return true;
        return false;
    }

    private static boolean courseCodeUsed(String c) {
        for (Course x : courses) if (x.getCourseCode().equalsIgnoreCase(c)) return true;
        return false;
    }

    private static boolean receiptUsed(String r) {
        for (FeePayment p : payments) if (p.getReceiptNumber().equalsIgnoreCase(r)) return true;
        return false;
    }

    private static boolean isbnUsed(String i) {
        for (LibraryBook b : books) if (b.getIsbn().equalsIgnoreCase(i)) return true;
        return false;
    }

    private static boolean activeEnrollmentExists(Student s, Course c, String semester) {
        for (Enrollment e : enrollments) {
            if (e.isActive() && e.getStudent() == s && e.getCourse() == c && e.getSemester().equals(semester)) return true;
        }
        return false;
    }

    private static void mustBeUnique(boolean used, String what) {
        if (used) throw new DuplicateRecordException(what + " already exists.");
    }

   
    private static List<Enrollment> activeEnrollments() {
        List<Enrollment> r = new ArrayList<>();
        for (Enrollment e : enrollments) if (e.isActive()) r.add(e);
        return r;
    }

    private static List<LibraryBook> availableBooks() {
        List<LibraryBook> r = new ArrayList<>();
        for (LibraryBook b : books) if (b.getAvailableCopies() > 0) r.add(b);
        return r;
    }

    private static List<BookLoan> activeLoans() {
        List<BookLoan> r = new ArrayList<>();
        for (BookLoan l : loans) if (l.isActive()) r.add(l);
        return r;
    }

    
    private static Object pickDepartment() { return pickRecord("Department", () -> departments, Main::registerDepartment, false); }
    private static Object pickTeacher() { return pickRecord("Teacher", () -> teachers, Main::registerTeacher, false); }
    private static Object pickStudent() { return pickRecord("Student", () -> students, Main::registerStudent, false); }
    private static Object pickCourse() { return pickRecord("Course", () -> courses, Main::registerCourse, false); }

    
    private static Object pickHeadTeacher() {
        if (teachers.isEmpty()) {
            System.out.println("  (No teacher exists yet - head teacher left empty; assign it later with Update Record.)");
            return null;
        }
        return pickRecord("Teacher", () -> teachers, Main::registerTeacher, true);
    }

    
    private static void personFields(Field[] f) {
        f[0] = text(1, "Person ID", "String | unique | 4-30 chars", uniqueP("Person ID", 4, 30, Main::idUsed));
        f[1] = text(1, "First name", "String | 2-60 chars", textP("First name", 2, 60));
        f[2] = text(1, "Last name", "String | 2-60 chars", textP("Last name", 2, 60));
        f[3] = text(1, "Email", "String | unique | email", uniqueEmailP("Email"));
        f[4] = text(1, "Phone", "String | 10-15 digits", phoneP("Phone"));
        f[5] = text(1, "Date of birth", "LocalDate | YYYY-MM-DD", dateP("Date of birth", true, false));
    }

    private static void academicFields(Field[] f) {
        f[6] = text(2, "Member number", "String | unique | 4-30", uniqueP("Member number", 4, 30, Main::memberNumberUsed));
        f[7] = pick(2, "Department", "Department | select", Main::pickDepartment);
        f[8] = text(2, "Join date", "LocalDate | YYYY-MM-DD", dateP("Join date", false, true));
        f[9] = text(2, "Academic status", choiceHint(AcademicMember.allowedStatuses()),
                choiceP("Academic status", AcademicMember.allowedStatuses()));
        f[10] = text(2, "Institutional email", "String | unique | email", uniqueEmailP("Institutional email"));
        f[11] = text(2, "Advisor name", "String | 2-120 chars", textP("Advisor name", 2, 120));
    }

    
    private static void checkPersonUnique(Person p) {
        mustBeUnique(idUsed(p.getId()), "ID " + p.getId());
        mustBeUnique(emailUsed(p.getEmail()), "Email " + p.getEmail());
        if (p instanceof AcademicMember m) {
            if (m.getInstitutionalEmail().equalsIgnoreCase(m.getEmail()))
                throw new ValidationException("Institutional email must differ from the personal email.");
            mustBeUnique(emailUsed(m.getInstitutionalEmail()), "Email " + m.getInstitutionalEmail());
            mustBeUnique(memberNumberUsed(m.getMemberNumber()), "Member number " + m.getMemberNumber());
        }
    }

    private static Object registerDepartment() {
        Field[] f = new Field[6];
        f[0] = text(1, "Department ID", "String | unique | 4-30", uniqueP("Department ID", 4, 30, Main::idUsed));
        f[1] = text(1, "Department name", "String | 2-60 chars", textP("Department name", 2, 60));
        f[2] = text(1, "Department code", "String | unique | 2-10", uniqueP("Department code", 2, 10, Main::deptCodeUsed));
        f[3] = text(2, "Office location", "String | 2-60 chars", textP("Office location", 2, 60));
        f[4] = text(2, "Contact email", "String | email", emailP("Contact email"));
        f[5] = pick(3, "Head teacher", "Teacher | optional", Main::pickHeadTeacher);
        String[] steps = {"Identity", "Location & contact", "Leadership", "Review & confirm"};
        return runForm("DEPARTMENT REGISTRATION", steps, f, () -> {
            Department d = new Department(str(f, 0), str(f, 1), str(f, 2), str(f, 3), str(f, 4), (Teacher) f[5].value);
            d.validate();
            mustBeUnique(idUsed(d.getId()), "ID " + d.getId());
            mustBeUnique(deptCodeUsed(d.getDepartmentCode()), "Department code " + d.getDepartmentCode());
            departments.add(d);
            System.out.println("+ Department " + d.getId() + " registered successfully.");
            return d;
        });
    }

    private static Object registerStudent() {
        Field[] f = new Field[18];
        personFields(f);
        academicFields(f);
        f[12] = text(3, "Student number", "String | unique | 4-30", uniqueP("Student number", 4, 30, Main::studentNumberUsed));
        f[13] = text(3, "Program name", "String | 2-120 chars", textP("Program name", 2, 120));
        f[14] = text(3, "Admission date", "LocalDate | YYYY-MM-DD", dateP("Admission date", false, true));
        f[15] = text(3, "Study year", "int | 1-6", intP("Study year", 1, 6));
        f[16] = text(3, "Guardian phone", "String | 10-15 digits", phoneP("Guardian phone"));
        f[17] = text(3, "Scholarship", "1 YES / 2 NO", yesNoP("Scholarship"));
        String[] steps = {"Personal details", "Academic details", "Student details", "Review & confirm"};
        return runForm("STUDENT REGISTRATION", steps, f, () -> {
            Student s = new Student(str(f, 0), str(f, 1), str(f, 2), str(f, 3), str(f, 4), date(f, 5),
                    str(f, 6), (Department) f[7].value, date(f, 8), str(f, 9), str(f, 10), str(f, 11),
                    str(f, 12), str(f, 13), date(f, 14), num(f, 15), str(f, 16), flag(f, 17));
            s.validate();
            checkPersonUnique(s);
            mustBeUnique(studentNumberUsed(s.getStudentNumber()), "Student number " + s.getStudentNumber());
            students.add(s);
            System.out.println("+ Student " + s.getId() + " registered successfully.");
            return s;
        });
    }

    private static Object registerTeacher() {
        Field[] f = new Field[18];
        personFields(f);
        academicFields(f);
        f[12] = text(3, "Employee number", "String | unique | 4-30", uniqueP("Employee number", 4, 30, Main::employeeNumberUsed));
        f[13] = text(3, "Specialization", "String | 2-120 chars", textP("Specialization", 2, 120));
        f[14] = text(3, "Hire date", "LocalDate | YYYY-MM-DD", dateP("Hire date", false, true));
        f[15] = text(3, "Salary", "BigDecimal | > 0", moneyP("Salary"));
        f[16] = text(3, "Office number", "String | 1-30 chars", textP("Office number", 1, 30));
        f[17] = text(3, "Employment type", choiceHint(Teacher.allowedEmploymentTypes()),
                choiceP("Employment type", Teacher.allowedEmploymentTypes()));
        String[] steps = {"Personal details", "Academic details", "Teacher details", "Review & confirm"};
        return runForm("TEACHER REGISTRATION", steps, f, () -> {
            Teacher t = new Teacher(str(f, 0), str(f, 1), str(f, 2), str(f, 3), str(f, 4), date(f, 5),
                    str(f, 6), (Department) f[7].value, date(f, 8), str(f, 9), str(f, 10), str(f, 11),
                    str(f, 12), str(f, 13), date(f, 14), money(f, 15), str(f, 16), str(f, 17));
            t.validate();
            checkPersonUnique(t);
            mustBeUnique(employeeNumberUsed(t.getEmployeeNumber()), "Employee number " + t.getEmployeeNumber());
            teachers.add(t);
            System.out.println("+ Teacher " + t.getId() + " registered successfully.");
            return t;
        });
    }

    private static Object registerAdministrator() {
        Field[] f = new Field[12];
        personFields(f);
        f[6] = text(2, "Staff number", "String | unique | 4-30", uniqueP("Staff number", 4, 30, Main::staffNumberUsed));
        f[7] = text(2, "Job title", "String | 2-80 chars", textP("Job title", 2, 80));
        f[8] = text(2, "Assigned office", "String | 1-60 chars", textP("Assigned office", 1, 60));
        f[9] = text(3, "Employment date", "LocalDate | YYYY-MM-DD", dateP("Employment date", false, true));
        f[10] = text(3, "Access level", "int | 1-5", intP("Access level", 1, 5));
        f[11] = text(3, "Active", "1 YES / 2 NO", yesNoP("Active"));
        String[] steps = {"Personal details", "Staff details", "Access details", "Review & confirm"};
        return runForm("ADMINISTRATOR REGISTRATION", steps, f, () -> {
            Administrator a = new Administrator(str(f, 0), str(f, 1), str(f, 2), str(f, 3), str(f, 4), date(f, 5),
                    str(f, 6), str(f, 7), str(f, 8), date(f, 9), num(f, 10), flag(f, 11));
            a.validate();
            checkPersonUnique(a);
            mustBeUnique(staffNumberUsed(a.getStaffNumber()), "Staff number " + a.getStaffNumber());
            administrators.add(a);
            System.out.println("+ Administrator " + a.getId() + " registered successfully.");
            return a;
        });
    }

    private static Object registerCourse() {
        Field[] f = new Field[6];
        f[0] = text(1, "Course ID", "String | unique | 4-30", uniqueP("Course ID", 4, 30, Main::idUsed));
        f[1] = text(1, "Course code", "String | unique | 2-20", uniqueP("Course code", 2, 20, Main::courseCodeUsed));
        f[2] = text(1, "Course title", "String | 2-120 chars", textP("Course title", 2, 120));
        f[3] = text(2, "Credit hours", "int | 1-6", intP("Credit hours", 1, 6));
        f[4] = pick(2, "Department", "Department | select", Main::pickDepartment);
        f[5] = pick(3, "Assigned teacher", "Teacher | select", Main::pickTeacher);
        String[] steps = {"Identity", "Credits & department", "Teacher", "Review & confirm"};
        return runForm("COURSE REGISTRATION", steps, f, () -> {
            Course c = new Course(str(f, 0), str(f, 1), str(f, 2), num(f, 3), (Department) f[4].value, (Teacher) f[5].value);
            c.validate();
            mustBeUnique(idUsed(c.getId()), "ID " + c.getId());
            mustBeUnique(courseCodeUsed(c.getCourseCode()), "Course code " + c.getCourseCode());
            courses.add(c);
            System.out.println("+ Course " + c.getId() + " registered successfully.");
            return c;
        });
    }

    private static Object registerClassroom() {
        Field[] f = new Field[6];
        f[0] = text(1, "Classroom ID", "String | unique | 4-30", uniqueP("Classroom ID", 4, 30, Main::idUsed));
        f[1] = text(1, "Building name", "String | 2-60 chars", textP("Building name", 2, 60));
        f[2] = text(1, "Room number", "String | 1-20 chars", textP("Room number", 1, 20));
        f[3] = text(2, "Capacity", "int | > 0", intP("Capacity", 1, 100000));
        f[4] = text(2, "Floor number", "int | >= 0", intP("Floor number", 0, 200));
        f[5] = text(3, "Available", "1 YES / 2 NO", yesNoP("Available"));
        String[] steps = {"Location", "Size", "Availability", "Review & confirm"};
        return runForm("CLASSROOM REGISTRATION", steps, f, () -> {
            Classroom c = new Classroom(str(f, 0), str(f, 1), str(f, 2), num(f, 3), num(f, 4), flag(f, 5));
            c.validate();
            mustBeUnique(idUsed(c.getId()), "ID " + c.getId());
            classrooms.add(c);
            System.out.println("+ Classroom " + c.getId() + " registered successfully.");
            return c;
        });
    }

    private static Object enrollStudent() {
        Field[] f = new Field[6];
        f[0] = text(1, "Enrollment ID", "String | unique | 4-30", uniqueP("Enrollment ID", 4, 30, Main::idUsed));
        f[1] = pick(1, "Student", "Student | select", Main::pickStudent);
        f[2] = pick(2, "Course", "Course | select", Main::pickCourse);
        f[3] = text(2, "Enrollment date", "LocalDate | YYYY-MM-DD", dateP("Enrollment date", false, true));
        f[4] = text(3, "Semester", choiceHint(Enrollment.allowedSemesters()), s -> {
            String sem = (String) choiceP("Semester", Enrollment.allowedSemesters()).parse(s);
            if (f[1].value != null && f[2].value != null
                    && activeEnrollmentExists((Student) f[1].value, (Course) f[2].value, sem))
                throw new DuplicateRecordException("This student already has an ACTIVE enrollment in that course for " + sem + ".");
            return sem;
        });
        f[5] = text(3, "Enrollment status", choiceHint(Enrollment.allowedStatuses()),
                choiceP("Enrollment status", Enrollment.allowedStatuses()));
        String[] steps = {"Student", "Course & date", "Semester & status", "Review & confirm"};
        return runForm("ENROLL STUDENT", steps, f, () -> {
            Student st = (Student) f[1].value;
            Course co = (Course) f[2].value;
            if (st == null || co == null) throw new PrerequisiteMissingException("A student and a course are required.");
            Enrollment e = new Enrollment(str(f, 0), st, co, date(f, 3), str(f, 4), str(f, 5));
            e.validate();
            mustBeUnique(idUsed(e.getId()), "ID " + e.getId());
            if (e.isActive() && activeEnrollmentExists(st, co, e.getSemester()))
                throw new DuplicateRecordException("Active enrollment for this student, course and semester");
            enrollments.add(e);
            System.out.println("+ Enrollment " + e.getId() + " registered successfully.");
            return e;
        });
    }

    private static Object recordAssessment() {
        Field[] f = new Field[6];
        f[0] = text(1, "Assessment ID", "String | unique | 4-30", uniqueP("Assessment ID", 4, 30, Main::idUsed));
        f[1] = pick(1, "Enrollment", "active Enrollment | select",
                () -> pickRecord("Active Enrollment", Main::activeEnrollments, Main::enrollStudent, false));
        f[2] = text(2, "Assessment type", choiceHint(Assessment.allowedTypes()),
                choiceP("Assessment type", Assessment.allowedTypes()));
        f[3] = text(2, "Maximum score", "double | > 0", s -> {
            try {
                double v = Double.parseDouble(s.trim());
                if (!Double.isFinite(v) || v <= 0) throw new ValidationException("Maximum score must be greater than zero.");
                return v;
            } catch (NumberFormatException e) {
                throw new ValidationException("Maximum score must be a number.");
            }
        });
        f[4] = text(3, "Obtained score", "double | 0 to maximum", s -> {
            try {
                double v = Double.parseDouble(s.trim());
                double max = (f[3].value == null) ? Double.MAX_VALUE : (Double) f[3].value;
                if (!Double.isFinite(v) || v < 0 || v > max)
                    throw new ValidationException("Obtained score must be between 0 and " + max + ".");
                return v;
            } catch (NumberFormatException e) {
                throw new ValidationException("Obtained score must be a number.");
            }
        });
        f[5] = text(3, "Assessment date", "LocalDate | YYYY-MM-DD", dateP("Assessment date", false, true));
        String[] steps = {"Enrollment", "Type & maximum", "Result & date", "Review & confirm"};
        return runForm("RECORD ASSESSMENT", steps, f, () -> {
            Enrollment en = (Enrollment) f[1].value;
            if (en == null) throw new PrerequisiteMissingException("An enrollment is required.");
            if (!en.isActive()) throw new PrerequisiteMissingException("Only an ACTIVE enrollment can be assessed.");
            Assessment a = new Assessment(str(f, 0), en, str(f, 2), dbl(f, 3), dbl(f, 4), date(f, 5));
            a.validate();
            mustBeUnique(idUsed(a.getId()), "ID " + a.getId());
            assessments.add(a);
            System.out.println("+ Assessment " + a.getId() + " recorded successfully.");
            return a;
        });
    }

    private static Object recordFeePayment() {
        Field[] f = new Field[6];
        f[0] = text(1, "Payment ID", "String | unique | 4-30", uniqueP("Payment ID", 4, 30, Main::idUsed));
        f[1] = pick(1, "Student", "Student | select", Main::pickStudent);
        f[2] = text(2, "Amount", "BigDecimal | > 0", moneyP("Amount"));
        f[3] = text(2, "Payment date", "LocalDate | YYYY-MM-DD", dateP("Payment date", false, true));
        f[4] = text(3, "Payment method", choiceHint(FeePayment.allowedMethods()),
                choiceP("Payment method", FeePayment.allowedMethods()));
        f[5] = text(3, "Receipt number", "String | unique | 3-30", uniqueP("Receipt number", 3, 30, Main::receiptUsed));
        String[] steps = {"Student", "Amount & date", "Method & receipt", "Review & confirm"};
        return runForm("RECORD FEE PAYMENT", steps, f, () -> {
            Student st = (Student) f[1].value;
            if (st == null) throw new PrerequisiteMissingException("A student is required.");
            FeePayment p = new FeePayment(str(f, 0), st, money(f, 2), date(f, 3), str(f, 4), str(f, 5));
            p.validate();
            mustBeUnique(idUsed(p.getId()), "ID " + p.getId());
            mustBeUnique(receiptUsed(p.getReceiptNumber()), "Receipt number " + p.getReceiptNumber());
            payments.add(p);
            System.out.println("+ Payment " + p.getId() + " recorded successfully.");
            return p;
        });
    }

    private static Object registerBook() {
        Field[] f = new Field[6];
        f[0] = text(1, "Book ID", "String | unique | 4-30", uniqueP("Book ID", 4, 30, Main::idUsed));
        f[1] = text(1, "ISBN", "String | unique | 10-17", uniqueP("ISBN", 10, 17, Main::isbnUsed));
        f[2] = text(1, "Title", "String | 2-120 chars", textP("Title", 2, 120));
        f[3] = text(2, "Author", "String | 2-100 chars", textP("Author", 2, 100));
        f[4] = text(2, "Total copies", "int | >= 0", intP("Total copies", 0, 100000));
        f[5] = text(3, "Available copies", "int | 0 to total", s -> {
            int max = (f[4].value == null) ? 100000 : (Integer) f[4].value;
            return intP("Available copies", 0, max).parse(s);
        });
        String[] steps = {"Identity", "Author & stock", "Availability", "Review & confirm"};
        return runForm("LIBRARY BOOK REGISTRATION", steps, f, () -> {
            LibraryBook b = new LibraryBook(str(f, 0), str(f, 1), str(f, 2), str(f, 3), num(f, 4), num(f, 5));
            b.validate();
            mustBeUnique(idUsed(b.getId()), "ID " + b.getId());
            mustBeUnique(isbnUsed(b.getIsbn()), "ISBN " + b.getIsbn());
            books.add(b);
            System.out.println("+ Book " + b.getId() + " registered successfully.");
            return b;
        });
    }

   
    private static Object borrowBook() {
        Field[] f = new Field[6];
        f[0] = text(1, "Loan ID", "String | unique | 4-30", uniqueP("Loan ID", 4, 30, Main::idUsed));
        f[1] = pick(1, "Student", "Student | select", Main::pickStudent);
        f[2] = pick(2, "Book", "Book with a free copy | select",
                () -> pickRecord("Available Book", Main::availableBooks, Main::registerBook, false));
        f[3] = text(2, "Borrowed date", "LocalDate | YYYY-MM-DD", dateP("Borrowed date", false, true));
        f[4] = text(3, "Due date", "LocalDate | after borrowed date", s -> {
            LocalDate d = Registrable.parseDate(s, "Due date");
            if (f[3].value != null && !d.isAfter((LocalDate) f[3].value))
                throw new ValidationException("Due date must be after the borrowed date (" + f[3].value + ").");
            return d;
        });
        f[5] = derived(3, "Returned date", null);
        String[] steps = {"Student", "Book & borrowed date", "Due & return", "Review & confirm"};
        return runForm("BORROW LIBRARY BOOK", steps, f, () -> {
            Student st = (Student) f[1].value;
            LibraryBook bk = (LibraryBook) f[2].value;
            if (st == null || bk == null) throw new PrerequisiteMissingException("A student and a book are required.");
            BookLoan loan = new BookLoan(str(f, 0), st, bk, date(f, 3), date(f, 4)); // uses this(...) chaining
            loan.validate();
            mustBeUnique(idUsed(loan.getId()), "ID " + loan.getId());
            if (bk.getAvailableCopies() <= 0)
                throw new BookUnavailableException("No copy of '" + bk.getTitle() + "' is available right now.");
            bk.setAvailableCopies(bk.getAvailableCopies() - 1);   // exactly once
            loans.add(loan);
            System.out.println("+ Loan " + loan.getId() + " created. Copies left: " + bk.getAvailableCopies());
            return loan;
        });
    }

    private static void returnBook() {
        Object r = pickRecord("Active Loan", Main::activeLoans, () -> runRegistration(Main::borrowBook), false);
        if (!(r instanceof BookLoan loan)) { System.out.println("Return cancelled."); return; }
        Field d = text(0, "Return date", "LocalDate | YYYY-MM-DD", s -> {
            LocalDate v = Registrable.parseDate(s, "Return date");
            Registrable.requireNotFuture(v, "Return date");
            if (v.isBefore(loan.getBorrowedDate()))
                throw new ValidationException("Return date cannot be before the borrowed date (" + loan.getBorrowedDate() + ").");
            return v;
        });
        System.out.println("Returning: " + loan.getDisplayName() + " (borrowed " + loan.getBorrowedDate() + ")");
        Object v = ask(d, 1);
        if (v == CANCEL || v == BACK) { System.out.println("Return cancelled. Nothing changed."); return; }
        if (!confirm("Confirm return on " + v + "?")) { System.out.println("Return cancelled. Nothing changed."); return; }
        LibraryBook bk = loan.getBook();
        if (bk.getAvailableCopies() >= bk.getTotalCopies())
            throw new ValidationException("Stock already full; cannot add another returned copy.");
        loan.setReturnedDate((LocalDate) v);
        bk.setAvailableCopies(bk.getAvailableCopies() + 1);       // exactly once
        System.out.println("+ Book returned. Copies available now: " + bk.getAvailableCopies() + "/" + bk.getTotalCopies());
    }

    
    private static final String[] TYPE_NAMES = {"Departments", "Students", "Teachers", "Administrators", "Courses",
            "Classrooms", "Enrollments", "Assessments", "Fee payments", "Library books", "Book loans"};

    private static List<? extends Identifiable> listFor(int n) {
        return switch (n) {
            case 1 -> departments;
            case 2 -> students;
            case 3 -> teachers;
            case 4 -> administrators;
            case 5 -> courses;
            case 6 -> classrooms;
            case 7 -> enrollments;
            case 8 -> assessments;
            case 9 -> payments;
            case 10 -> books;
            case 11 -> loans;
            default -> allRecords();
        };
    }

    private static void viewAllRecords() {
        while (true) {
            System.out.println("\nView which records?");
            for (int i = 0; i < TYPE_NAMES.length; i++) System.out.println(" " + (i + 1) + ". " + TYPE_NAMES[i]);
            System.out.println(" 12. Everything");
            System.out.println("  0. Back to main menu");
            System.out.print("Select option: ");
            int n;
            try {
                n = Integer.parseInt(line().trim());
            } catch (NumberFormatException e) {
                System.out.println("! Please type a number.");
                continue;
            }
            if (n == 0) return;
            if (n < 1 || n > 12) { System.out.println("! Choose 0-12."); continue; }
            List<? extends Identifiable> list = listFor(n);
            String title = (n == 12) ? "Everything" : TYPE_NAMES[n - 1];
            if (list.isEmpty()) {
                System.out.println("(" + title + ": no records yet.)");
            } else {
                System.out.println("--- " + title + " (" + list.size() + ") ---");
                for (Identifiable r : list) System.out.println(" " + ((Registrable) r).getRegistrationSummary());
            }
        }
    }

    private static Identifiable findById(String id) {
        for (Identifiable r : allRecords()) if (r.getId().equalsIgnoreCase(id)) return r;
        throw new RecordNotFoundException("No record found with ID '" + id + "'.");
    }

    private static void searchById() {
        System.out.print("Enter the ID to search (0 = back): ");
        String id = line().trim();
        if (id.equals("0")) return;
        if (id.isEmpty()) throw new ValidationException("ID cannot be blank.");
        Identifiable r = findById(id);
        System.out.println("Found: " + ((Registrable) r).getRegistrationSummary());
        ((Registrable) r).printDetails();
    }

    private static Field ed(String label, String hint, Object current, Parser p, Action a) {
        Field f = new Field(0, label, hint, p, null);
        f.value = current;
        f.apply = a;
        return f;
    }

   
    private static List<Field> editableFields(Identifiable rec) {
        List<Field> e = new ArrayList<>();
        if (rec instanceof Person p) {
            e.add(ed("Phone", "10-15 digits", p.getPhoneNumber(), phoneP("Phone"), v -> p.setPhoneNumber((String) v)));
            e.add(ed("Email", "unique email", p.getEmail(), uniqueEmailP("Email"), v -> p.setEmail((String) v)));
        }
        if (rec instanceof AcademicMember m) {
            e.add(ed("Advisor name", "2-120 chars", m.getAdvisorName(), textP("Advisor name", 2, 120), v -> m.setAdvisorName((String) v)));
            e.add(ed("Academic status", choiceHint(AcademicMember.allowedStatuses()), m.getAcademicStatus(),
                    choiceP("Academic status", AcademicMember.allowedStatuses()), v -> m.setAcademicStatus((String) v)));
        }
        if (rec instanceof Student s) {
            e.add(ed("Program name", "2-120 chars", s.getProgramName(), textP("Program name", 2, 120), v -> s.setProgramName((String) v)));
            e.add(ed("Study year", "1-6", s.getStudyYear(), intP("Study year", 1, 6), v -> s.setStudyYear((Integer) v)));
            e.add(ed("Guardian phone", "10-15 digits", s.getGuardianPhone(), phoneP("Guardian phone"), v -> s.setGuardianPhone((String) v)));
            e.add(ed("Scholarship", "1 YES / 2 NO", s.isScholarshipStatus(), yesNoP("Scholarship"), v -> s.setScholarshipStatus((Boolean) v)));
        }
        if (rec instanceof Teacher t) {
            e.add(ed("Specialization", "2-120 chars", t.getSpecialization(), textP("Specialization", 2, 120), v -> t.setSpecialization((String) v)));
            e.add(ed("Salary", "> 0", t.getSalary(), moneyP("Salary"), v -> t.setSalary((BigDecimal) v)));
            e.add(ed("Office number", "1-30 chars", t.getOfficeNumber(), textP("Office number", 1, 30), v -> t.setOfficeNumber((String) v)));
            e.add(ed("Employment type", choiceHint(Teacher.allowedEmploymentTypes()), t.getEmploymentType(),
                    choiceP("Employment type", Teacher.allowedEmploymentTypes()), v -> t.setEmploymentType((String) v)));
        }
        if (rec instanceof Administrator a) {
            e.add(ed("Job title", "2-80 chars", a.getJobTitle(), textP("Job title", 2, 80), v -> a.setJobTitle((String) v)));
            e.add(ed("Assigned office", "1-60 chars", a.getAssignedOffice(), textP("Assigned office", 1, 60), v -> a.setAssignedOffice((String) v)));
            e.add(ed("Access level", "1-5", a.getAccessLevel(), intP("Access level", 1, 5), v -> a.setAccessLevel((Integer) v)));
            e.add(ed("Active", "1 YES / 2 NO", a.isActive(), yesNoP("Active"), v -> a.setActive((Boolean) v)));
        }
        if (rec instanceof Department d) {
            e.add(ed("Department name", "2-60 chars", d.getDepartmentName(), textP("Department name", 2, 60), v -> d.setDepartmentName((String) v)));
            e.add(ed("Office location", "2-60 chars", d.getOfficeLocation(), textP("Office location", 2, 60), v -> d.setOfficeLocation((String) v)));
            e.add(ed("Contact email", "email", d.getContactEmail(), emailP("Contact email"), v -> d.setContactEmail((String) v)));
            Field head = new Field(0, "Head teacher", "Teacher | select (S = clear)", null,
                    () -> pickRecord("Teacher", () -> teachers, Main::registerTeacher, true));
            head.value = d.getHeadTeacher();
            head.apply = v -> d.setHeadTeacher((Teacher) v);
            e.add(head);
        }
        if (rec instanceof Course c) {
            e.add(ed("Course title", "2-120 chars", c.getCourseTitle(), textP("Course title", 2, 120), v -> c.setCourseTitle((String) v)));
            e.add(ed("Credit hours", "1-6", c.getCreditHours(), intP("Credit hours", 1, 6), v -> c.setCreditHours((Integer) v)));
        }
        if (rec instanceof Classroom c) {
            e.add(ed("Capacity", "> 0", c.getCapacity(), intP("Capacity", 1, 100000), v -> c.setCapacity((Integer) v)));
            e.add(ed("Floor number", ">= 0", c.getFloorNumber(), intP("Floor number", 0, 200), v -> c.setFloorNumber((Integer) v)));
            e.add(ed("Available", "1 YES / 2 NO", c.isAvailable(), yesNoP("Available"), v -> c.setAvailable((Boolean) v)));
        }
        if (rec instanceof Enrollment en) {
            e.add(ed("Enrollment status", choiceHint(Enrollment.allowedStatuses()), en.getEnrollmentStatus(),
                    choiceP("Enrollment status", Enrollment.allowedStatuses()), v -> en.setEnrollmentStatus((String) v)));
        }
        if (rec instanceof Assessment a) {
            e.add(ed("Assessment type", choiceHint(Assessment.allowedTypes()), a.getAssessmentType(),
                    choiceP("Assessment type", Assessment.allowedTypes()), v -> a.setAssessmentType((String) v)));
            e.add(ed("Obtained score", "0 to " + a.getMaximumScore(), a.getObtainedScore(), s -> {
                try {
                    double v = Double.parseDouble(s.trim());
                    if (!Double.isFinite(v) || v < 0 || v > a.getMaximumScore())
                        throw new ValidationException("Obtained score must be between 0 and " + a.getMaximumScore() + ".");
                    return v;
                } catch (NumberFormatException ex) {
                    throw new ValidationException("Obtained score must be a number.");
                }
            }, v -> a.setObtainedScore((Double) v)));
        }
        if (rec instanceof FeePayment p) {
            e.add(ed("Payment method", choiceHint(FeePayment.allowedMethods()), p.getPaymentMethod(),
                    choiceP("Payment method", FeePayment.allowedMethods()), v -> p.setPaymentMethod((String) v)));
        }
        if (rec instanceof LibraryBook b) {
            e.add(ed("Title", "2-120 chars", b.getTitle(), textP("Title", 2, 120), v -> b.setTitle((String) v)));
            e.add(ed("Author", "2-100 chars", b.getAuthor(), textP("Author", 2, 100), v -> b.setAuthor((String) v)));
            int out = b.getTotalCopies() - b.getAvailableCopies();   // copies currently on loan
            e.add(ed("Total copies", ">= " + out + " (copies on loan)", b.getTotalCopies(),
                    intP("Total copies", out, 100000), v -> {
                        int newTotal = (Integer) v;
                        int onLoan = b.getTotalCopies() - b.getAvailableCopies();
                        b.setTotalCopies(newTotal);
                        b.setAvailableCopies(newTotal - onLoan);
                    }));
        }
        if (rec instanceof BookLoan l) {
            e.add(ed("Due date", "after " + l.getBorrowedDate(), l.getDueDate(), s -> {
                LocalDate d = Registrable.parseDate(s, "Due date");
                if (!d.isAfter(l.getBorrowedDate())) throw new ValidationException("Due date must be after the borrowed date.");
                return d;
            }, v -> l.setDueDate((LocalDate) v)));
        }
        return e;
    }

    private static void updateRecord() {
        System.out.print("Enter the ID of the record to update (0 = back): ");
        String id = line().trim();
        if (id.equals("0")) return;
        if (id.isEmpty()) throw new ValidationException("ID cannot be blank.");
        Identifiable rec = findById(id);
        Registrable reg = (Registrable) rec;
        List<Field> eds = editableFields(rec);
        while (true) {
            System.out.println("\nEditing: " + reg.getRegistrationSummary());
            for (int i = 0; i < eds.size(); i++) {
                System.out.printf(" %02d %-18s : %s%n", i + 1, eds.get(i).label, show(eds.get(i).value));
            }
            System.out.println(" 00 Done (the ID cannot be changed)");
            System.out.print("Field number to edit: ");
            int n;
            try {
                n = Integer.parseInt(line().trim());
            } catch (NumberFormatException e) {
                System.out.println("! Please type a field number.");
                continue;
            }
            if (n == 0) { System.out.println("Update finished."); return; }
            if (n < 1 || n > eds.size()) { System.out.println("! No such field."); continue; }
            Field x = eds.get(n - 1);
            System.out.println("Old value: " + show(x.value));
            Object newValue = ask(x, n);
            if (newValue == CANCEL || newValue == BACK) { System.out.println("Edit abandoned."); continue; }
            if (!confirm("Change " + x.label + " from '" + show(x.value) + "' to '" + show(newValue) + "'?")) {
                System.out.println("No change made.");
                continue;
            }
            Object old = x.value;
            x.apply.apply(newValue);
            try {
                reg.validate();                     
                x.value = newValue;
                System.out.println("+ Updated successfully.");
            } catch (ValidationException ex) {
                x.apply.apply(old);                  // roll back
                System.out.println("! Change rejected and undone: " + ex.getMessage());
            }
        }
    }

    private static void studentSummary() {
        Object r = pickStudent();
        if (!(r instanceof Student s)) { System.out.println("Cancelled."); return; }
        System.out.println("\n=========== STUDENT ACADEMIC SUMMARY ===========");
        s.printDetails();
        System.out.println("--- Enrollments & assessments ---");
        boolean any = false;
        double totalPercent = 0;
        int count = 0;
        for (Enrollment e : enrollments) {
            if (e.getStudent() != s) continue;
            any = true;
            System.out.println(" " + e.getCourse().getDisplayName() + " | " + e.getSemester() + " | " + e.getEnrollmentStatus());
            for (Assessment a : assessments) {
                if (a.getEnrollment() != e) continue;
                double pct = a.getObtainedScore() / a.getMaximumScore() * 100;
                System.out.printf("     %-10s %.1f / %.1f  (%.1f%%)  on %s%n", a.getAssessmentType(),
                        a.getObtainedScore(), a.getMaximumScore(), pct, a.getAssessmentDate());
                totalPercent += pct;
                count++;
            }
        }
        if (!any) System.out.println(" No enrollments yet.");
        if (count > 0) System.out.printf("Average assessment result: %.1f%%%n", totalPercent / count);
        else System.out.println("No assessments recorded yet.");
    }

    private static void paymentHistory() {
        Object r = pickStudent();
        if (!(r instanceof Student s)) { System.out.println("Cancelled."); return; }
        System.out.println("\n--- Payment history for " + s.getDisplayName() + " ---");
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (FeePayment p : payments) {
            if (p.getStudent() != s) continue;
            System.out.println(" " + p.getPaymentDate() + " | " + p.getAmount() + " | " + p.getPaymentMethod()
                    + " | receipt " + p.getReceiptNumber());
            total = total.add(p.getAmount());
            count++;
        }
        if (count == 0) System.out.println(" No payments recorded for this student.");
        else System.out.println("Total paid: " + total + " (" + count + " payment(s))");
    }

    private static void showAvailableBooks() {
        List<LibraryBook> list = availableBooks();
        if (list.isEmpty()) { System.out.println("No book currently has an available copy."); return; }
        System.out.println("--- Available books ---");
        for (LibraryBook b : list) {
            System.out.println(" [" + b.getId() + "] " + b.getTitle() + " by " + b.getAuthor()
                    + " - " + b.getAvailableCopies() + "/" + b.getTotalCopies() + " available");
        }
    }

    
    private static void demonstratePolymorphism() {
        System.out.println("\n===== 1) SUPERCLASS POLYMORPHISM: List<Person> =====");
        List<Person> people = allPeople();
        if (people.isEmpty()) {
            System.out.println("No Student/Teacher/Administrator registered yet.");
        }
        for (Person p : people) {
            // same call, different behaviour at runtime (overridden methods)
            System.out.println(p.getRoleDescription() + " -> " + p.getRegistrationSummary() + " | active=" + p.isActive());
            if (p instanceof AcademicMember m) {
                System.out.println("    academic category (AcademicMember): " + m.getAcademicCategory());
            }
        }
        System.out.println("\n===== 2) INTERFACE POLYMORPHISM: List<Identifiable> =====");
        List<Identifiable> items = allRecords();
        if (items.isEmpty()) System.out.println("No records yet.");
        for (Identifiable i : items) {
            System.out.println(i.getId() + " | " + i.getDisplayName() + " | active=" + i.isActive());
        }
        System.out.println("\n===== NOTES =====");
        System.out.println("* Person and AcademicMember are abstract: they contain abstract methods (getRoleDescription,");
        System.out.println("  getAcademicCategory) with no body, so 'new Person(...)' would be meaningless and is refused by the compiler.");
        System.out.println("* Multilevel inheritance: Person -> AcademicMember -> Student / Teacher.");
        System.out.println("* Hierarchical inheritance: Student, Teacher (and Administrator) share parents.");
        System.out.println("* Constructor chaining: Student -> super(...) -> AcademicMember -> super(...) -> Person;");
        System.out.println("  BookLoan and Enrollment also use this(...) between their own constructors.");
    }
}
