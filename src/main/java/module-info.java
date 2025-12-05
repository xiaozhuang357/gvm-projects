module com._404.wms {
	requires javafx.controls;
	requires javafx.fxml;
	requires transitive javafx.graphics;
	requires javafx.base;
	requires java.sql;
	requires com.google.gson;
	requires java.logging;

	opens com._404.wms to javafx.fxml;

	exports com._404.wms;
	exports com._404.wms.model;
	exports com._404.wms.network;
	exports com._404.wms.service;
	exports com._404.wms.config;
	exports com._404.wms.db;
	exports com._404.wms.db.connection;
	exports com._404.wms.db.util;
	exports com._404.wms.dao;
	exports com._404.wms.dao.impl;
}