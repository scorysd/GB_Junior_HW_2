
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TestProcessor {

    /**
     * Данный метод находит все void методы без аргументов в классе, и запускеет их.
     * <p>
     * Для запуска создается тестовый объект с помощью конструткора без аргументов.
     */
    public static void runTest(Class<?> testClass) {
        final Constructor<?> declaredConstructor;
        try {
            declaredConstructor = testClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Для класса \"" + testClass.getName() + "\" не найден конструктор без аргументов");
        }

        final Object testObj;
        try {
            testObj = declaredConstructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Не удалось создать объект класса \"" + testClass.getName() + "\"");
        }

        List<Method> methods = new ArrayList<>();
        for (Method method : testClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Test.class) && !method.isAnnotationPresent(Skip.class)) {
                checkTestMethod(method);
                methods.add(method);
            }
        }

        /*
        Стрим, выводящий метеоды, имеющие исключительно BeforeEach аннотацию (перед всеми тестами)
         */
        methods.stream().filter(it -> it.isAnnotationPresent(BeforeEach.class)).forEach(it -> runTest(it, testObj));

        /*
        Стрим, выводящий метеоды, в порядке возрастания знеачения поля order (за исключением методов,
        имеющих аннтоции BeforeEach и AfterEach)
         */
        methods.stream().filter(it -> !it.isAnnotationPresent(BeforeEach.class) & !it.isAnnotationPresent(AfterEach.class))
                .sorted(new Comparator<Method>() {
                    @Override
                    public int compare(Method o1, Method o2) {
                        if (o1.getAnnotation(Test.class).order() > o2.getAnnotation(Test.class).order()) {
                            return 1;
                        } else if (o1.getAnnotation(Test.class).order() < o2.getAnnotation(Test.class).order()) {
                            return -1;
                        }
                        return 0;
                    }
                })
                .forEach(it -> runTest(it, testObj));

        /*
        Стрим, выводящий метеоды, имеющие исключительно AfterEach аннотацию (после всеми тестами)
         */
        methods.stream().filter(it -> it.isAnnotationPresent(AfterEach.class)).forEach(it -> runTest(it, testObj));
    }

    private static void checkTestMethod(Method method) {
        if (!method.getReturnType().isAssignableFrom(void.class) || method.getParameterCount() != 0) {
            throw new IllegalArgumentException("Метод \"" + method.getName() + "\" должен быть void и не иметь аргументов");
        }
    }

    private static void runTest(Method testMethod, Object testObj) {
        try {
            testMethod.invoke(testObj);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Не удалось запустить тестовый метод \"" + testMethod.getName() + "\"");
        } catch (AssertionError e) {

        }
    }

}
