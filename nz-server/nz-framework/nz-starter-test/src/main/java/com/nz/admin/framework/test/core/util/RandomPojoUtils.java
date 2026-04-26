package com.nz.admin.framework.test.core.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * 随机 POJO 构造工具。
 */
public final class RandomPojoUtils {

    private RandomPojoUtils() {
    }

    /**
     * 随机构造并填充一个 POJO 对象。
     *
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 随机对象
     */
    public static <T> T randomPojo(Class<T> clazz) {
        T instance = newInstance(clazz);
        fillFields(instance, clazz);
        return instance;
    }

    /**
     * 随机构造并填充一个 POJO 对象，并执行自定义赋值。
     *
     * @param clazz       目标类型
     * @param customizers 自定义赋值逻辑
     * @param <T>         泛型
     * @return 随机对象
     */
    @SafeVarargs
    public static <T> T randomPojo(Class<T> clazz, Consumer<T>... customizers) {
        T instance = randomPojo(clazz);
        if (customizers == null) {
            return instance;
        }
        for (Consumer<T> customizer : customizers) {
            if (customizer != null) {
                customizer.accept(instance);
            }
        }
        return instance;
    }

    /**
     * 创建支持链式 set 的随机 POJO 构造器。
     *
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 随机 POJO 构造器
     */
    public static <T> RandomPojoBuilder<T> randomPojoBuilder(Class<T> clazz) {
        return new RandomPojoBuilder<>(randomPojo(clazz));
    }

    /**
     * 随机构造并填充多个 POJO 对象。
     *
     * @param clazz 目标类型
     * @param size  数量
     * @param <T>   泛型
     * @return 随机对象列表
     */
    public static <T> List<T> randomPojoList(Class<T> clazz, int size) {
        if (size <= 0) {
            return List.of();
        }
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(randomPojo(clazz));
        }
        return result;
    }

    private static <T> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("randomPojo 创建对象失败: " + clazz.getName(), e);
        }
    }

    private static void fillFields(Object target, Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field[] fields = current.getDeclaredFields();
            for (Field field : fields) {
                if (shouldSkip(field)) {
                    continue;
                }
                try {
                    if (!field.canAccess(target)) {
                        field.setAccessible(true);
                    }
                    field.set(target, randomValue(field.getType(), field.getName()));
                } catch (Exception e) {
                    throw new IllegalStateException("randomPojo 字段填充失败: " + field.getName(), e);
                }
            }
            current = current.getSuperclass();
        }
    }

    private static boolean shouldSkip(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers);
    }

    private static Object randomValue(Class<?> fieldType, String fieldName) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (fieldType == String.class) {
            return fieldName + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        if (fieldType == Integer.class || fieldType == int.class) {
            return random.nextInt(1, 1000);
        }
        if (fieldType == Long.class || fieldType == long.class) {
            return random.nextLong(1, 100_000);
        }
        if (fieldType == Double.class || fieldType == double.class) {
            return random.nextDouble(1.0, 1000.0);
        }
        if (fieldType == Float.class || fieldType == float.class) {
            return random.nextFloat() * 1000.0F;
        }
        if (fieldType == Short.class || fieldType == short.class) {
            return (short) random.nextInt(1, Short.MAX_VALUE);
        }
        if (fieldType == Byte.class || fieldType == byte.class) {
            return (byte) random.nextInt(1, Byte.MAX_VALUE);
        }
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            return random.nextBoolean();
        }
        if (fieldType == BigDecimal.class) {
            return BigDecimal.valueOf(random.nextDouble(1.0, 1000.0)).setScale(2, RoundingMode.HALF_UP);
        }
        if (fieldType == LocalDateTime.class) {
            return LocalDateTime.now().minusDays(random.nextInt(0, 30));
        }
        if (fieldType == LocalDate.class) {
            return LocalDate.now().minusDays(random.nextInt(0, 30));
        }
        if (fieldType == LocalTime.class) {
            return LocalTime.now().minusSeconds(random.nextInt(0, 3600));
        }
        if (fieldType.isEnum()) {
            Object[] constants = fieldType.getEnumConstants();
            if (constants != null && constants.length > 0) {
                return constants[random.nextInt(constants.length)];
            }
        }
        return null;
    }

    /**
     * 支持链式 set 的随机 POJO 构造器。
     *
     * @param <T> 泛型
     */
    public static final class RandomPojoBuilder<T> {

        private final T value;

        private RandomPojoBuilder(T value) {
            this.value = value;
        }

        /**
         * 链式设置字段。
         *
         * @param setter 字段 setter 方法引用
         * @param fieldValue 字段值
         * @param <V> 字段类型
         * @return 当前构造器
         */
        public <V> RandomPojoBuilder<T> set(BiConsumer<T, V> setter, V fieldValue) {
            setter.accept(value, fieldValue);
            return this;
        }

        /**
         * 返回最终对象。
         *
         * @return 最终对象
         */
        public T build() {
            return value;
        }
    }
}
