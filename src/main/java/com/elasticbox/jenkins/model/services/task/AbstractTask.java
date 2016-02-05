package com.elasticbox.jenkins.model.services.task;

/**
 * Created by serna on 12/6/15.
 */
public abstract class AbstractTask<R> implements Task<R> {

    protected R result = null;

    protected abstract void performExecute() throws TaskException;

    public R getResult() {
        return result;
    }


}