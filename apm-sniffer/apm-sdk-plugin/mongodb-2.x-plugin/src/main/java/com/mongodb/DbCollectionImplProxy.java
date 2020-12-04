package com.mongodb;

import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.pt.FlagValue;

import java.lang.reflect.Field;

/**
 * @description:
 * @author: renxl
 * @create: 2020-12-04 14:46
 */
public class DbCollectionImplProxy {
    public static void process(EnhancedInstance objInst) {
        if (FlagValue.isPt()) {

            isPressureTest(objInst);
        } else {
            isNotPressureTest(objInst);

        }


    }



    private static void isNotPressureTest(EnhancedInstance objInst) {
        if (objInst instanceof DBCollectionImpl) {
            // 集合名
            // 数据库名+点符号连接+集合名
            String _fullName = ((DBCollectionImpl) objInst)._fullName.replaceAll(FlagValue.PT_ROUTE_PREFIX,"");
            DBApiLayer db = (DBApiLayer) ((DBCollectionImpl) objInst).getDB();
            // 数据库名
            String _dbName = db._name.replaceAll(FlagValue.PT_ROUTE_PREFIX,"");
            // 数据库名
            String dbName = db._root.replaceAll(FlagValue.PT_ROUTE_PREFIX,"");
            // 数据库名+点
            String _rootPlusDot = db._rootPlusDot.replaceAll(FlagValue.PT_ROUTE_PREFIX,"");


            try {
                // DBCollectionImpl 数据库名+ 集合名
                Class<?> clazz = Class.forName("com.mongodb.DBCollectionImpl");
                Field namespaceField = clazz.getDeclaredField("namespace");
                namespaceField.setAccessible(true);
                namespaceField.set(objInst, _fullName);

                Field _fullNameField = clazz.getDeclaredField("_fullName");
                _fullNameField.setAccessible(true);
                _fullNameField.set(objInst, _fullName);



                // DBApiLayer
                Class<?> clazz1 = Class.forName("com.mongodb.DBApiLayer");
                Field _nameField = clazz1.getDeclaredField("_name");
                _nameField.setAccessible(true);
                _nameField.set(db, _dbName);


                Field _rootField = clazz1.getDeclaredField("_root");
                _rootField.setAccessible(true);
                _rootField.set(db,  dbName);

                Field _rootPlusDotField = clazz1.getDeclaredField("_rootPlusDot");
                _rootPlusDotField.setAccessible(true);
                _rootPlusDotField.set(db, _rootPlusDot);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private static void isPressureTest(EnhancedInstance objInst) {
        if (objInst instanceof DBCollectionImpl) {
            // 集合名
            // 数据库名+点符号连接+集合名
            String _fullName = ((DBCollectionImpl) objInst)._fullName;


            DBApiLayer db = (DBApiLayer) ((DBCollectionImpl) objInst).getDB();
            // 数据库名
            String _dbName = db._name;
            // 数据库名
            String dbName = db._root;
            // 数据库名+点
            String _rootPlusDot = db._rootPlusDot;

            // 说明压测流量已经处理过
            if(_fullName.contains(FlagValue.PT_ROUTE_PREFIX)){
                return;
            }

            try {
                // DBCollectionImpl 数据库名+ 集合名
                Class<?> clazz = Class.forName("com.mongodb.DBCollectionImpl");
                Field namespaceField = clazz.getDeclaredField("namespace");
                namespaceField.setAccessible(true);
                namespaceField.set(objInst, FlagValue.PT_ROUTE_PREFIX+_fullName);

                Field _fullNameField = clazz.getDeclaredField("_fullName");
                _fullNameField.setAccessible(true);
                _fullNameField.set(objInst, FlagValue.PT_ROUTE_PREFIX + _fullName);

                // DBApiLayer
                Class<?> clazz1 = Class.forName("com.mongodb.DBApiLayer");
                Field _nameField = clazz1.getDeclaredField("_name");
                _nameField.setAccessible(true);
                _nameField.set(db, FlagValue.PT_ROUTE_PREFIX + _dbName);


                Field _rootField = clazz1.getDeclaredField("_root");
                _rootField.setAccessible(true);
                _rootField.set(db, FlagValue.PT_ROUTE_PREFIX + dbName);

                Field _rootPlusDotField = clazz1.getDeclaredField("_rootPlusDot");
                _rootPlusDotField.setAccessible(true);
                _rootPlusDotField.set(db, FlagValue.PT_ROUTE_PREFIX + _rootPlusDot);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
