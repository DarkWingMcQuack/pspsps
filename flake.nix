{
  description = "pspsps — fluent Scala syntax for Cats, Cats Effect, and FS2";

  inputs = {
    typelevel-nix.url = "github:typelevel/typelevel-nix";
    nixpkgs.follows = "typelevel-nix/nixpkgs";
    flake-utils.follows = "typelevel-nix/flake-utils";
  };

  outputs =
    {
      nixpkgs,
      flake-utils,
      typelevel-nix,
      ...
    }:
    flake-utils.lib.eachSystem [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ] (
      system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ typelevel-nix.overlays.default ];
        };

        mkShell =
          jdk:
          pkgs.devshell.mkShell {
            imports = [ typelevel-nix.typelevelShell ];
            name = "pspsps";
            typelevelShell = {
              jdk.package = jdk;
              nodejs.enable = true;
              nodejs.package = pkgs.nodejs_24;
              native.enable = false;
            };
            packages = [ pkgs.git ];
          };
      in
      {
        devShells = {
          default = mkShell pkgs.jdk17;
          jdk21 = mkShell pkgs.jdk21;
        };

        formatter = pkgs.nixfmt;
      }
    );
}
