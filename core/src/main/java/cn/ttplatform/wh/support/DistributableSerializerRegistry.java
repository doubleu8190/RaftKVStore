package cn.ttplatform.wh.support;

import cn.ttplatform.wh.exception.UnknownTypeException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Wang Hao
 * @date 2021/4/29 0:45
 */
public class DistributableSerializerRegistry implements Registry<DistributableSerializer> {

    private static final int COUNT_OF_FACTORY = 18;
    private final Map<Integer, DistributableSerializer> factoryMap;

    public DistributableSerializerRegistry() {
        this.factoryMap = new HashMap<>((int) (COUNT_OF_FACTORY / 0.75f + 1));
    }

    @Override
    public void register(DistributableSerializer factory) {
        factoryMap.put(factory.getFactoryType(), factory);
    }

    public DistributableSerializer getSerializer(int type) {
        DistributableSerializer factory = factoryMap.get(type);
        if (factory == null) {
            throw new UnknownTypeException("unknown message type[" + type + "]");
        }
        return factory;
    }
}
