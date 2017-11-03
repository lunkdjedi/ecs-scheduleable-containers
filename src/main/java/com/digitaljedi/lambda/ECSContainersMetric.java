package com.digitaljedi.lambda;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.ListContainerInstancesRequest;
import com.amazonaws.services.ecs.model.ListContainerInstancesResult;
import com.amazonaws.services.ecs.model.Resource;

public class ECSContainersMetric {

	private int CONTAINER_MAX_CPU=128;
	private int CONTAINER_MAX_MEM=512;
	
	public Map<String,Integer> handler() {
		Map<String,Integer> results = new HashMap<String,Integer>();
		AmazonECS ecs = AmazonECSClientBuilder.defaultClient();
		List<String> clusterARns = ecs.listClusters().getClusterArns();
		for (String cluster : clusterARns) {
			String[] clusterSplit = cluster.split("/");
			String clusterName = clusterSplit[clusterSplit.length-1];
			ListContainerInstancesRequest containerInstancesRequest = new ListContainerInstancesRequest();
			containerInstancesRequest.setCluster(cluster);
			containerInstancesRequest.setStatus("ACTIVE");
			ListContainerInstancesResult containerInstancesResult =	ecs.listContainerInstances(containerInstancesRequest);
			List<String> containerArns = containerInstancesResult.getContainerInstanceArns();
			
			DescribeContainerInstancesRequest describeContainerInstancesRequest = new DescribeContainerInstancesRequest();
			describeContainerInstancesRequest.setCluster(cluster);
			describeContainerInstancesRequest.setContainerInstances(containerArns);
			DescribeContainerInstancesResult containerInstanceResult = ecs.describeContainerInstances(describeContainerInstancesRequest);
			
			List<ContainerInstance> instances = containerInstanceResult.getContainerInstances();
			for (ContainerInstance instance : instances) {
				List<Resource> resources = instance.getRemainingResources();
				int containers_by_cpu=0;
				int containers_by_mem=0;
				for (Resource resource : resources) {
					if (resource.getName().equals("CPU"))
						containers_by_cpu = resource.getIntegerValue()/CONTAINER_MAX_CPU;
					
					if (resource.getName().equals("MEMORY"))
						containers_by_mem = resource.getIntegerValue()/CONTAINER_MAX_MEM;
				}
				System.out.println("CPU Containers: " + containers_by_cpu);
				System.out.println("Memory Containers: " + containers_by_mem);
				int schedulable_containers = Math.min(containers_by_cpu, containers_by_mem);
				results.put(clusterName, schedulable_containers);
				
				Dimension dimension = new Dimension();
				dimension.setName("ClusterName");
				dimension.setValue(clusterName);
				MetricDatum datum = new MetricDatum();
				datum.setMetricName("SchedulableContainers");
				datum.setTimestamp(new Date());
				datum.setValue(new Double(schedulable_containers));
				List<Dimension> dimensions = new ArrayList<Dimension>();
				dimensions.add(dimension);
				datum.setDimensions(dimensions);
				
				AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();
				PutMetricDataRequest putMetricDataRequest = new PutMetricDataRequest();
				putMetricDataRequest.setNamespace("AWS/ECS");
				List<MetricDatum> metrics = new ArrayList<MetricDatum>();
				metrics.add(datum);
				putMetricDataRequest.setMetricData(metrics);
				cw.putMetricData(putMetricDataRequest);
			}
		}
		return results;
		
	}
	
}
