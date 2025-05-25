package com.example.school_management.commons.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.Map;

public class QueryParamsArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return QueryParams.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter param,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest req = webRequest.getNativeRequest(HttpServletRequest.class);
        QueryParams qp = new QueryParams();

        // include=students,courses
        assert req != null;
        String inc = req.getParameter("include");
        if (inc != null && !inc.isBlank()) {
            qp.getInclude().addAll(Arrays.asList(inc.split(",")));
        }

        // fields[entity]=a,b,c  &  filter[attr]=v1,v2
        for (Map.Entry<String,String[]> e : req.getParameterMap().entrySet()) {
            String key = e.getKey();
            String val = e.getValue()[0];
            if (key.startsWith("fields[")) {
                String entity = key.substring(7, key.length() - 1);
                if (!val.isBlank()) {
                    qp.getFields().put(entity, Arrays.asList(val.split(",")));
                }
            } else if (key.startsWith("filter[")) {
                String attr = key.substring(7, key.length() - 1);
                if (!val.isBlank()) {
                    qp.getFilters().put(attr, Arrays.asList(val.split(",")));
                }
            }
        }

        // sort=name,asc;createdAt,desc
        String sortParam = req.getParameter("sort");
        if (sortParam != null && !sortParam.isBlank()) {
            for (String segment : sortParam.split(";")) {
                String[] parts = segment.split(",", 2);
                String property = parts[0].trim();
                Sort.Direction dir = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc"))
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
                qp.getSort().add(new Sort.Order(dir, property));
            }
        }

        // pagination: page & size
        String pageParam = req.getParameter("page");
        String sizeParam = req.getParameter("size");
        qp.setPage(pageParam != null ? Integer.parseInt(pageParam) : 0);
        qp.setSize(sizeParam != null ? Integer.parseInt(sizeParam) : 20);

        return qp;
    }
}
