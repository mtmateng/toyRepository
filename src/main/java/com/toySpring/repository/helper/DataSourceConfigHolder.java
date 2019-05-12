package com.toySpring.repository.helper;

import lombok.Data;

@Data
public class DataSourceConfigHolder {

    String url;
    String driver;
    String username;
    String password;

}
