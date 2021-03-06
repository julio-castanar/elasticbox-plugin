package com.elasticbox.jenkins.model.services.deployment;

import com.elasticbox.ApiClient;
import com.elasticbox.jenkins.model.box.AbstractBox;
import com.elasticbox.jenkins.model.instance.Instance;
import com.elasticbox.jenkins.model.repository.DeploymentOrderRepository;
import com.elasticbox.jenkins.model.repository.InstanceRepository;
import com.elasticbox.jenkins.model.repository.WorkspaceRepository;
import com.elasticbox.jenkins.model.repository.api.BoxRepositoryApiImpl;
import com.elasticbox.jenkins.model.repository.api.DeploymentOrderRepositoryApiImpl;
import com.elasticbox.jenkins.model.repository.api.InstanceRepositoryApiImpl;
import com.elasticbox.jenkins.model.repository.api.WorkspacesRepositoryApiImpl;
import com.elasticbox.jenkins.model.services.deployment.configuration.policies.AbstractDeploymentDataPoliciesHandler;
import com.elasticbox.jenkins.model.services.deployment.execution.context.AbstractBoxDeploymentContext;
import com.elasticbox.jenkins.model.services.deployment.execution.deployers.BoxDeployer;
import com.elasticbox.jenkins.model.services.deployment.execution.deployers.BoxDeployerFactory;
import com.elasticbox.jenkins.model.services.deployment.execution.order.DeployBoxOrderResult;
import com.elasticbox.jenkins.model.box.policy.PolicyBox;
import com.elasticbox.jenkins.model.repository.BoxRepository;
import com.elasticbox.jenkins.model.repository.error.RepositoryException;
import com.elasticbox.jenkins.model.services.error.ServiceException;
import com.elasticbox.jenkins.model.workspace.AbstractWorkspace;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is how we decided to encapsulate the business logic. So, this service model the deploy box
 * user case.
 */
public class DeployBoxOrderServiceImpl implements DeployBoxOrderService {

    private static final Logger logger = Logger.getLogger(DeployBoxOrderServiceImpl.class.getName());

    private final InstanceRepository instanceRepository;
    private final DeploymentOrderRepository deploymentOrderRepository;
    private final BoxRepository boxRepository;
    private final WorkspaceRepository workspacesRepository;

    public DeployBoxOrderServiceImpl(ApiClient client) {

        this.boxRepository = new BoxRepositoryApiImpl(client);
        this.instanceRepository = new InstanceRepositoryApiImpl(client);
        this.deploymentOrderRepository =  new DeploymentOrderRepositoryApiImpl(client);
        this.workspacesRepository = new WorkspacesRepositoryApiImpl(client);
    }

    @Override
    public DeploymentType deploymentType(String boxToDeploy) throws ServiceException {

        try {
            final AbstractBox box = boxRepository.getBox(boxToDeploy);
            final DeploymentType type = DeploymentType.findBy(box);
            return type;

        } catch (RepositoryException e) {
            logger.log(Level.SEVERE, "Impossible to retrieve box: " + boxToDeploy);
            throw new ServiceException("Impossible to retrieve box: " + boxToDeploy);
        }
    }

    @Override
    public DeployBoxOrderResult<List<AbstractBox>> updateableBoxes(String workspace) throws ServiceException {

        try {
            final List<AbstractBox> updateableBoxes = boxRepository.getNoPolicyAndNoApplicationBoxes(workspace);
            return new DeployBoxOrderResult<List<AbstractBox>>(updateableBoxes);

        } catch (RepositoryException e) {
            logger.log(
                    Level.SEVERE,
                    "Impossible to retrieve updateable boxes (no policies neither application boxes) for workspace: "
                            + workspace);

            throw new ServiceException(
                    "Impossible to retrieve updateable boxes (no policies neither application boxes) for workspace: "
                            + workspace);
        }
    }


    @Override
    public DeployBoxOrderResult<List<AbstractBox>> getBoxesToDeploy(String workspace) throws ServiceException {
        try {
            final List<AbstractBox> noPolicyBoxes = boxRepository.getNoPolicyBoxes(workspace);
            return new DeployBoxOrderResult<List<AbstractBox>>(noPolicyBoxes);

        } catch (RepositoryException e) {
            logger.log(Level.SEVERE, "Impossible to get boxes o deploy for workspace: " + workspace);
            throw new ServiceException("Impossible to get boxes to deploy for workspace: " + workspace);
        }
    }


    @Override
    public DeployBoxOrderResult<List<PolicyBox>> deploymentPolicies(String workspace, String boxToDeploy)
            throws ServiceException {

        try {
            final AbstractBox box = boxRepository.getBox(boxToDeploy);

            final List<PolicyBox> policies =
                    AbstractDeploymentDataPoliciesHandler.getPolicies(boxRepository, workspace, box);

            return new DeployBoxOrderResult<List<PolicyBox>>(policies);

        } catch (RepositoryException e) {

            logger.log(
                    Level.SEVERE,
                    "Impossible to get policies for workspace: " + workspace + ", box: " + boxToDeploy);

            throw new ServiceException(
                    "Impossible to get policies for workspace: " + workspace + ", box: " + boxToDeploy);
        }

    }

    @Override
    public DeployBoxOrderResult<AbstractWorkspace> findWorkspaceOrFirstByDefault(String workspace)
            throws ServiceException {

        try {
            final AbstractWorkspace workspaceOrFirstByDefault =
                    workspacesRepository.findWorkspaceOrFirstByDefault(workspace);

            return new DeployBoxOrderResult<AbstractWorkspace>(workspaceOrFirstByDefault);

        } catch (RepositoryException e) {
            logger.log(Level.SEVERE, "Impossible retrieve workspaces", e);
            throw new ServiceException("Impossible retrieve workspaces",e);
        }
    }


    @Override
    public DeployBoxOrderResult<List<AbstractWorkspace>> getWorkspaces() throws ServiceException {

        try {
            final List<AbstractWorkspace> workspaces = workspacesRepository.getWorkspaces();
            return new DeployBoxOrderResult<List<AbstractWorkspace>>(workspaces);

        } catch (RepositoryException e) {
            logger.log(Level.SEVERE, "Impossible retrieve workspaces");
            throw new ServiceException("Impossible retrieve workspaces");
        }
    }

    public <T extends AbstractBoxDeploymentContext> DeployBoxOrderResult<List<Instance>> deploy(T context)
            throws ServiceException {

        context.setBoxRepository(boxRepository);
        context.setDeploymentOrderRepository(deploymentOrderRepository);
        context.setInstanceRepository(instanceRepository);

        final BoxDeployer boxDeployer = BoxDeployerFactory.createBoxDeployer(context);
        try {
            context.setBoxRepository(boxRepository);

            final List<Instance> instancesDeployed = boxDeployer.deploy(context);
            return  new DeployBoxOrderResult<List<Instance>>(instancesDeployed);

        } catch (RepositoryException e) {
            logger.log(Level.SEVERE, "Deployment error, deploy order: " + context.getOrder());
            throw new ServiceException("Deployment error, deploy order: " + context.getOrder());
        }
    }

    public DeployBoxOrderResult<AbstractBox> findBoxOrFirstByDefault(String workspace, String box) {

        try {
            final AbstractBox boxOrFirstByDefault = boxRepository.findBoxOrFirstByDefault(workspace, box);
            return new DeployBoxOrderResult<AbstractBox>(boxOrFirstByDefault);

        } catch (RepositoryException e) {
            logger.log(Level.SEVERE, "Error getting box: " + box + " for workspace: " + workspace, e);
            throw new ServiceException("Error getting box: " + box + " for workspace: " + workspace, e);
        }
    }
}
