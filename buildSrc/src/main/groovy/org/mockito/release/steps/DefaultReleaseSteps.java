package org.mockito.release.steps;

import org.mockito.release.util.operations.Operation;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class DefaultReleaseSteps implements ReleaseSteps {

    private final List<ReleaseStep> steps = new ArrayList<ReleaseStep>();

    public ConfigurableReleaseStep newStep(String description, Operation operation) {
        DefaultReleaseStep step = new DefaultReleaseStep(description, operation);
        steps.add(step);
        return step;
    }

  public ReleaseStep getStep(int stepNumber) {
    return steps.get(stepNumber - 1); //TODO SF array index
  }

  public void performRollback() {
    rollback(steps);
  }

  public void perform() {
        System.out.println("Performing " + steps.size() + " release steps");
        LinkedList<ReleaseStep> attempted = new LinkedList<ReleaseStep>();
        for (ReleaseStep step : steps) {
            attempted.add(step);
            System.out.println("Step " + attempted.size() + ": " + step.getDescription());
            try {
                step.perform();
            } catch (Throwable t) {
                //Printing stack trace here is not very nice.
                //However, Gradle swallows the cause of the exception so it's better to have the info than not have it
                t.printStackTrace();

                throw new RuntimeException("Release failed at step " + attempted.size() + " (" + step.getDescription() + "). " +
                        "Rollback was not performed for the failed operation. Previous successful operations will be rolled back.", t);
            }
        }
        //TODO SF needs tidy up. I should model better the cleanup VS rollback operation
        while(!attempted.isEmpty()) {
          ReleaseStep step = attempted.removeLast();
          step.performCleanup();
        }
    }

    private static void rollback(List<ReleaseStep> attempted) {
        System.out.println("Release failed. Rolling back " + attempted.size() + " release steps.");
        LinkedList<ReleaseStep> targets = new LinkedList<ReleaseStep>(attempted);
        while(!targets.isEmpty()) {
            ReleaseStep s = targets.removeLast();
            //TODO SF push this message down
            System.out.println("Attempting to roll back step " + (targets.size() + 1) + " (" + s.getDescription() + ")");
            s.performRollback();
        }
    }
}