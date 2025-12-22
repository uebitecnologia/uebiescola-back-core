package br.com.uebiescola.core.presentation.dto;

public record AddressRequest(String zipCode, String street, String number, String neighborhood, String city, String state, String phone) {}
