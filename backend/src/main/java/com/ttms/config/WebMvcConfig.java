package com.ttms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Web MVC 配置
 * 配置静态资源映射 + Vue SPA 路由回退
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path:./uploads/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map /uploads/** URLs to the upload directory on disk
        String absolutePath = new java.io.File(uploadPath).getAbsolutePath();
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath);

        // SPA 路由回退：非 /api/、非静态文件的请求全部返回 index.html
        // 这样 Vue Router 的 /admin/orders 等路径刷新时才不会 404
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        // /api/ 路径不拦截，交给后端 Controller
                        if (resourcePath.startsWith("api/")) {
                            return null;
                        }
                        Resource resource = location.createRelative(resourcePath);
                        // 文件存在就返回，不存在就回退到 index.html（Vue Router 处理）
                        if (resource.exists() && resource.isReadable()) {
                            return resource;
                        }
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}
