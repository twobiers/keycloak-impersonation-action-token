{ pkgs, lib, config, inputs, ... }:

{
  packages = [ pkgs.git ];

  languages.java.enable = true;
}
