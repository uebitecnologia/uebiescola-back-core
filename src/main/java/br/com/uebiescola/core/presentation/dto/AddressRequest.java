package br.com.uebiescola.core.presentation.dto;

public record AddressRequest(String zipCode, String street, String complement, String number, String neighborhood, String city, String state, String phone, String mobile) {}
