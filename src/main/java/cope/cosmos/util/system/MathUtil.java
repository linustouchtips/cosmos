package cope.cosmos.util.system;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.Object2FloatFunction;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * @author linustouchtips
 * @since 05/06/2021
 */
public class MathUtil {

    /**
     * Rounds a double to the nearest decimal scale
     * @param number The double to round
     * @param scale The decimal points
     * @return The rounded double
     */
    public static double roundDouble(double number, int scale) {
        BigDecimal bigDecimal = new BigDecimal(number);

        // round
        bigDecimal = bigDecimal.setScale(scale, RoundingMode.HALF_UP);
        return bigDecimal.doubleValue();
    }

    /**
     * Rounds a float to the nearest decimal scale
     * @param number The float to round
     * @param scale The decimal points
     * @return The rounded float
     */
    public static float roundFloat(double number, int scale) {
        BigDecimal bigDecimal = BigDecimal.valueOf(number);

        // round
        bigDecimal = bigDecimal.setScale(scale, RoundingMode.FLOOR);
        return bigDecimal.floatValue();
    }

    public static <T> int sumOfInt(Collection<T> objs, Object2IntFunction<T> function) {
        int sum = 0;
        for (T obj: objs) {
            sum += function.getInt(obj);
        }
        return sum;
    }

    public static <T> float sumOfFloat(Collection<T> objs, Object2FloatFunction<T> function) {
        float sum = 0;
        for (T obj: objs) {
            sum += function.getFloat(obj);
        }
        return sum;
    }

    public static <T> int maxOfInt(Collection<T> objs, Object2IntFunction<T> function) {
        int max = Integer.MIN_VALUE;
        for (T obj: objs) {
            int value = function.getInt(obj);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static <T> float maxOfFloat(Collection<T> objs, Object2FloatFunction<T> function) {
        float max = Float.MIN_VALUE;
        for (T obj: objs) {
            float value = function.getFloat(obj);
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static float average(FloatArrayList values) {
        if (values.size() == 0) {
            return 0;
        }
        float sum = 0;
        for (float value: values) {
            sum += value;
        }
        return sum / values.size();
    }

    public static double roundOnSet(double value, int places) {
        final double scale = Math.pow(10.0, places);
        return Math.round(value * scale) / scale;
    }
}
