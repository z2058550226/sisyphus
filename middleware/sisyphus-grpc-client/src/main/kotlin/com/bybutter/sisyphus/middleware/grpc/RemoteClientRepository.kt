package com.bybutter.sisyphus.middleware.grpc

import com.bybutter.sisyphus.rpc.CallOptionsInterceptor
import com.bybutter.sisyphus.spring.BeanUtils
import io.grpc.CallOptions
import io.grpc.ClientInterceptor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBeansOfType
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.core.env.Environment

class RemoteClientRepository : ClientRepository {

    override var order: Int = Int.MAX_VALUE - 1000

    override fun listClientBeanDefinition(beanFactory: ConfigurableListableBeanFactory, environment: Environment): List<AbstractBeanDefinition> {
        val properties = beanFactory.getBeansOfType<GrpcChannelProperty>()
        if (properties.isEmpty()) return arrayListOf()
        val beanDefinitionList = arrayListOf<AbstractBeanDefinition>()
        for (property in properties.values) {
            val channel = createGrpcChannel(property.target, beanFactory)
            beanFactory.registerSingleton(property.name, channel)
            for (service in property.services) {
                val client = getClientFromService(service)
                val stub = getStubFromService(service)
                val clientBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(client as Class<Any>) {
                    val builderInterceptors = BeanUtils.getSortedBeans(beanFactory, ClientBuilderInterceptor::class.java)
                    val clientInterceptors = BeanUtils.getSortedBeans(beanFactory, ClientInterceptor::class.java)
                    val optionsInterceptors = BeanUtils.getSortedBeans(beanFactory, CallOptionsInterceptor::class.java)
                    interceptStub(createGrpcClient(stub, channel, optionsInterceptors.values, CallOptions.DEFAULT), builderInterceptors.values, clientInterceptors.values)
                }
                beanDefinitionList.add(clientBeanDefinition.beanDefinition)
            }
        }
        return beanDefinitionList
    }
}
