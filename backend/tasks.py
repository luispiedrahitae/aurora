from invoke import task


@task
def pip_compile(c):
    c.run(
        "pip-compile requirements/main.in --generate-hashes --strip-extras --pip-args '--progress-bar on' --quiet"
    )


@task
def pip_sync(c):
    c.run("pip-sync requirements/main.txt --pip-args '--progress-bar on'")


@task(pip_compile, pip_sync)
def pip_update(c): ...

@task
def get_changed_git_files(c):
    c.run("git diff --name-only main | grep '\.py$'")


@task
def lint_check(c):
    c.run("git diff --name-only --diff-filter d origin/main -- | grep '\.py$' | xargs ruff check --statistics --show-files")


@task
def lint_format(c):
    c.run("git diff --name-only --diff-filter d origin/main -- | grep '\.py$' | xargs ruff format --statistics --show-files")