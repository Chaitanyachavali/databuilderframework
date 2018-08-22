package com.flipkart.databuilderframework.engine;

import com.flipkart.databuilderframework.model.*;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The executor for a {@link com.flipkart.databuilderframework.model.DataFlow}.
 */
public abstract class DataFlowExecutor {
    private static final Logger logger = LoggerFactory.getLogger(DataFlowExecutor.class.getSimpleName());
    protected List<DataBuilderExecutionListener> dataBuilderExecutionListener;
    private final DataBuilderFactory dataBuilderFactory;

    public DataFlowExecutor(DataBuilderFactory dataBuilderFactory) {
        this.dataBuilderExecutionListener = Lists.newArrayList();
        this.dataBuilderFactory = dataBuilderFactory;
    }

    /**
     * The executor will use the builder factory in the DataFlow.
     */
    public DataFlowExecutor() {
        this.dataBuilderExecutionListener = Lists.newArrayList();
        this.dataBuilderFactory = null;
    }

    /**
     * Execute a data flow built using {@link com.flipkart.databuilderframework.engine.DataFlowBuilder}.
     * This should be used when using the framework in single request context.
     * @param dataFlow A data-flow built using {@link com.flipkart.databuilderframework.engine.DataFlowBuilder}.
     * @param data The {@link com.flipkart.databuilderframework.model.Data} objects to be used as input to execute this flow.
     * @return A response containing responses from every {@link DataBuilder}
     * @throws DataBuilderFrameworkException
     */
    public DataExecutionResponse run(final DataFlow dataFlow, Data... data) throws DataBuilderFrameworkException, DataValidationException {
        return run(dataFlow, new DataDelta(data));
    }

    /**
     * Execute a data flow built using {@link com.flipkart.databuilderframework.engine.DataFlowBuilder}.
     * This should be used when using the framework in single request context.
     * @param dataFlow A data-flow built using {@link com.flipkart.databuilderframework.engine.DataFlowBuilder}.
     * @param dataDelta A {@link com.flipkart.databuilderframework.model.DataDelta} of objects to be used as input to execute this flow.
     * @return A response containing responses from every {@link DataBuilder}.
     * @throws DataBuilderFrameworkException
     */
    public DataExecutionResponse run(final DataFlow dataFlow,
                                     DataDelta dataDelta) throws DataBuilderFrameworkException, DataValidationException {
        Preconditions.checkNotNull(dataFlow);
        Preconditions.checkArgument(null != dataFlow.getDataBuilderFactory() || null != dataBuilderFactory);
        return process(new DataBuilderContext(), new DataFlowInstance(), dataDelta, dataFlow, dataFlow.getDataBuilderFactory());
    }

    /**
     * It uses {@link com.flipkart.databuilderframework.model.Data} present in the existing
     * {@link com.flipkart.databuilderframework.model.DataSet} and those provided by
     * {@link com.flipkart.databuilderframework.model.DataDelta} to generate more data.
     * Non-transient {@link com.flipkart.databuilderframework.model.Data} generated by all executors invoked in a request
     * are registerd back into the {@link com.flipkart.databuilderframework.model.DataSet}
     *
     * @param dataFlowInstance An instance of the {@link com.flipkart.databuilderframework.model.DataFlow} to run.
     * @param data             The additional set of data to be considered for execution.
     * @return A response containing responses from every {@link DataBuilder}
     * that was invoked in this stage. Note that these have already been added to the DataSet before returning.
     * @throws DataBuilderFrameworkException
     */
    public DataExecutionResponse run(DataFlowInstance dataFlowInstance, Data... data) throws DataBuilderFrameworkException, DataValidationException {
        return run(dataFlowInstance, new DataDelta(data));
    }

    /**
     * It uses {@link com.flipkart.databuilderframework.model.Data} present in the existing
     * {@link com.flipkart.databuilderframework.model.DataSet} and those provided by
     * {@link com.flipkart.databuilderframework.model.DataDelta} to generate more data.
     * Non-transient {@link com.flipkart.databuilderframework.model.Data} generated by all executors invoked in a request
     * are registerd back into the {@link com.flipkart.databuilderframework.model.DataSet}
     *
     * @param dataFlowInstance An instance of the {@link com.flipkart.databuilderframework.model.DataFlow} to run.
     * @param dataDelta        The additional set of data to be considered for execution.
     * @return A response containing responses from every {@link DataBuilder}
     * that was invoked in this stage. Note that these have already been added to the DataSet before returning.
     * @throws DataBuilderFrameworkException
     */
    public DataExecutionResponse run(DataFlowInstance dataFlowInstance, DataDelta dataDelta) throws DataBuilderFrameworkException,DataValidationException {
        DataBuilderContext dataBuilderContext = DataBuilderContext.builder()
                .dataSet(dataFlowInstance.getDataSet())
                .contextData(Maps.newHashMap())
                .build();
        return run(dataBuilderContext, dataFlowInstance, dataDelta);
    }

    /**
     * It uses {@link com.flipkart.databuilderframework.model.Data} present in the existing
     * {@link com.flipkart.databuilderframework.model.DataSet} and those provided by
     * {@link com.flipkart.databuilderframework.model.DataDelta} to generate more data.
     * {@link com.flipkart.databuilderframework.model.Data} generated by all executors invoked in a request
     * are registerd back into the {@link com.flipkart.databuilderframework.model.DataSet}
     *
     * @param dataBuilderContext An instance of the {@link com.flipkart.databuilderframework.engine.DataBuilderContext} object.
     * @param dataFlowInstance   An instance of the {@link com.flipkart.databuilderframework.model.DataFlow} to run.
     * @param dataDelta          The set of data to be considered for analysis.
     * @return A response containing responses from every {@link DataBuilder}
     * that was invoked in this stage. Note that these have already been added to the DataSet before returning.
     * @throws DataBuilderFrameworkException
     */
    public DataExecutionResponse run(DataBuilderContext dataBuilderContext,
                                              DataFlowInstance dataFlowInstance,
                                              DataDelta dataDelta) throws DataBuilderFrameworkException, DataValidationException {
        DataFlow dataFlow = dataFlowInstance.getDataFlow();
        Preconditions.checkArgument(null != dataFlow.getDataBuilderFactory()
                || null != dataBuilderFactory);
        DataBuilderFactory builderFactory = dataFlow.getDataBuilderFactory();
        if(null == builderFactory) {
            builderFactory = dataBuilderFactory;
        }
        if(null == builderFactory) {
            throw new DataBuilderFrameworkException(DataBuilderFrameworkException.ErrorCode.NO_FACTORY_FOR_DATA_BUILDER,
                                                "No builder specified in contructor or dataflow");
        }
        return process(dataBuilderContext, dataFlowInstance, dataDelta, dataFlow, builderFactory);
    }

    protected DataExecutionResponse process(DataBuilderContext dataBuilderContext,
                                            DataFlowInstance dataFlowInstance,
                                            DataDelta dataDelta,
                                            DataFlow dataFlow,
                                            DataBuilderFactory builderFactory) throws DataBuilderFrameworkException, DataValidationException {
        DataExecutionResponse response = null;
        Throwable frameworkException = null;
        try {
            for (DataBuilderExecutionListener listener : dataBuilderExecutionListener) {
                try {
                    listener.preProcessing(dataFlowInstance, dataDelta);
                } catch (Throwable t) {
                    logger.error("Error running pre-processing listener: ", t);
                    throw new DataBuilderFrameworkException(DataBuilderFrameworkException.ErrorCode.PRE_PROCESSING_ERROR,
                            "Error running pre-processing listener: " +  t.getMessage());
                }
            }
            response = run(dataBuilderContext, dataFlowInstance, dataDelta, dataFlow, builderFactory);
            return response;
        } catch (DataBuilderFrameworkException e) {
            frameworkException = e;
            throw e;
        } finally {
            for (DataBuilderExecutionListener listener : dataBuilderExecutionListener) {
                try {
                    listener.postProcessing(dataFlowInstance, dataDelta, response, frameworkException);
                } catch (Throwable t) {
                    logger.error("Error running post-processing listener: ", t);
                }
            }
        }
    }

    abstract protected DataExecutionResponse run(DataBuilderContext dataBuilderContext,
                                                 DataFlowInstance dataFlowInstance,
                                                 DataDelta dataDelta,
                                                 DataFlow dataFlow,
                                                 DataBuilderFactory builderFactory) throws DataBuilderFrameworkException, DataValidationException;

    /**
     * A instance of {@link com.flipkart.databuilderframework.engine.DataBuilderExecutionListener}
     * that will be sent events when a builder is executed. This can be called multiple times with different listeners.
     * They will be called in order.
     *
     * @param listener Register a listener to be invoked during execution.
     */
    public void registerExecutionListener(DataBuilderExecutionListener listener) {
        dataBuilderExecutionListener.add(listener);
    }
}
