package encora.winterframework.util;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

class JSONParserTest {

    private static final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    public static class Job {

        private String title;

        private double salary;

        private byte someByte;

        private int[] someArray;

        private String[] directs;

        private Person manager;

        public Job() {}

        public Job(String title, double salary, byte someByte, int[] someArray, String[] directs,
            Person manager) {
            this.title = title;
            this.salary = salary;
            this.someByte = someByte;
            this.someArray = someArray;
            this.directs = directs;
            this.manager = manager;
        }

        @Override public String toString() {
            return "Job{" +
                "title='" + title + '\'' +
                ", salary=" + salary +
                ", someByte=" + someByte +
                ", someArray=" + Arrays.toString(someArray) +
                ", directs=" + Arrays.deepToString(directs) +
                //                ", manager=" + manager +
                '}';
        }
    }

    public static class Person {

        private String name;

        private String[] pronouns;

        private int age;

        private char sex;

        private Job job;

        private Job[] jobs;

        private boolean tenure;

        private boolean[][] array2d;

        private String[][][] array3d;

        private Date hireDate;

        public Person() {}

        public Person(String name, String[] pronouns, int age, char sex, Job job, Job[] jobs, boolean tenure, boolean[][] array2d,
            String[][][] array3d, Date hireDate) {
            this.name = name;
            this.pronouns = pronouns;
            this.age = age;
            this.sex = sex;
            this.job = job;
            this.jobs = jobs;
            this.tenure = tenure;
            this.array2d = array2d;
            this.array3d = array3d;
            this.hireDate = hireDate;
        }

        @Override public String toString() {
            return "Person{" +
                "name='" + name + '\'' +
                ", pronouns=" + Arrays.toString(pronouns) +
                ", age=" + age +
                ", sex=" + sex +
                ", job=" + job +
                ", jobs=" + Arrays.toString(jobs) +
                ", tenure=" + tenure +
                ", array2d=" + Arrays.toString(array2d) +
                ", array3d=" + Arrays.toString(array3d) +
                ", hireDate=" + hireDate +
                '}';
        }
    }

    public static void main(String[] args)
        throws IllegalAccessException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, InstantiationException {

        Job job = new Job("Java, 'a', Student ", 10.5, (byte) 100, new int[3], null, null);
        Person person =
            new Person("Ricardo ,B'a,r'ajas,", new String[] { "his", "him", null }, 29, 'M', job, new Job[] { job, job }, true,
                new boolean[][] { { true, false }, { false, true } },
                new String[][][] {
                    null,
                    { { "1", "1" } },
                    { { "2", "2" }, null, { "2", "2" } },
                    { { "3", null }, { "3", "3" }, { "3", "3" }, null }
                },
                null
            );
        // job.manager = person; -- Uncomment for infinite recursion
        log.info(person.toString());
        log.info("\n//First to JSON");
        String personJSON = JSONParser.toJSON(person);
        log.info(personJSON);

        log.info("\n//Now to person");
        Person newPerson = JSONParser.toObject(personJSON, Person.class);
        log.info(newPerson.toString());

        personJSON = JSONParser.toJSON(person);
        log.info("\n//To JSON again");
        log.info(personJSON);
    }
}