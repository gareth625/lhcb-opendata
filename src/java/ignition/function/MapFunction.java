package ignition.function;
import java.util.Iterator;
import java.lang.Object;
import java.util.Collection;

import clojure.lang.IFn;

public class MapFunction extends sparkling.serialization.AbstractSerializableWrappedIFn implements org.apache.spark.api.java.function.MapFunction {

    public MapFunction(IFn func) {
        super(func);
    }

  @SuppressWarnings("unchecked")
  public Object call(Object v1) throws Exception {
      return f.invoke(v1);
  }

}
